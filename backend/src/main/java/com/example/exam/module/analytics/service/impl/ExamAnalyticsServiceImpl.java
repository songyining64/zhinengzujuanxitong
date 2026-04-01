package com.example.exam.module.analytics.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.analytics.dto.ExamKnowledgeStatDTO;
import com.example.exam.module.analytics.dto.ExamOverviewDTO;
import com.example.exam.module.analytics.dto.ExamQuestionStatDTO;
import com.example.exam.module.analytics.dto.ExamRankExportRow;
import com.example.exam.module.analytics.dto.StudentRankDTO;
import com.example.exam.module.analytics.service.ExamAnalyticsService;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.exam.entity.Exam;
import com.example.exam.module.exam.entity.ExamAnswer;
import com.example.exam.module.exam.entity.ExamRecord;
import com.example.exam.module.exam.mapper.ExamAnswerMapper;
import com.example.exam.module.exam.mapper.ExamMapper;
import com.example.exam.module.exam.mapper.ExamRecordMapper;
import com.example.exam.module.exam.service.impl.ExamServiceImpl;
import com.example.exam.module.paper.entity.PaperQuestion;
import com.example.exam.module.paper.mapper.PaperQuestionMapper;
import com.example.exam.module.question.entity.KnowledgePoint;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.mapper.KnowledgePointMapper;
import com.example.exam.module.question.mapper.QuestionMapper;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.mapper.UserMapper;
import com.example.exam.module.system.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamAnalyticsServiceImpl implements ExamAnalyticsService {

    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final UserMapper userMapper;
    private final CoursePermissionService coursePermissionService;
    private final UserService userService;

    private Exam requireExam(Long examId) {
        Exam e = examMapper.selectById(examId);
        if (e == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试不存在");
        }
        return e;
    }

    /** 学生端：成绩未发布时禁止查看统计与排名 */
    private void assertStudentScorePublishedIfNeeded(Exam exam) {
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null) {
            return;
        }
        if (RoleEnum.STUDENT.name().equals(u.getRole())) {
            if (exam.getScorePublished() == null || exam.getScorePublished() == 0) {
                throw new BizException(ErrorCode.FORBIDDEN, "成绩未发布，暂不可查看统计与排名");
            }
        }
    }

    @Override
    public ExamOverviewDTO overview(Long examId) {
        Exam exam = requireExam(examId);
        coursePermissionService.assertExamReadable(examId);
        assertStudentScorePublishedIfNeeded(exam);
        List<ExamRecord> submitted = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStatus, ExamServiceImpl.RS_SUBMITTED));
        ExamOverviewDTO dto = new ExamOverviewDTO();
        dto.setExamId(examId);
        dto.setSubmittedCount(submitted.size());
        dto.setTotalRecords(submitted.size());
        dto.setPassScore(exam.getPassScore());
        dto.setScorePublished(exam.getScorePublished() != null && exam.getScorePublished() != 0);
        long passCount = 0;
        if (exam.getPassScore() != null) {
            for (ExamRecord r : submitted) {
                if (r.getTotalScore() != null && r.getTotalScore().compareTo(exam.getPassScore()) >= 0) {
                    passCount++;
                }
            }
        }
        dto.setPassCount(passCount);
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal max = null;
        BigDecimal min = null;
        for (ExamRecord r : submitted) {
            if (r.getTotalScore() == null) {
                continue;
            }
            sum = sum.add(r.getTotalScore());
            max = max == null ? r.getTotalScore() : max.max(r.getTotalScore());
            min = min == null ? r.getTotalScore() : min.min(r.getTotalScore());
        }
        if (!submitted.isEmpty() && submitted.stream().anyMatch(x -> x.getTotalScore() != null)) {
            long c = submitted.stream().filter(x -> x.getTotalScore() != null).count();
            dto.setAvgScore(sum.divide(BigDecimal.valueOf(c), 4, RoundingMode.HALF_UP));
        } else {
            dto.setAvgScore(BigDecimal.ZERO);
        }
        dto.setMaxScore(max);
        dto.setMinScore(min);
        return dto;
    }

    @Override
    public Page<StudentRankDTO> rank(Long examId, long page, long size) {
        Exam exam = requireExam(examId);
        coursePermissionService.assertExamReadable(examId);
        assertStudentScorePublishedIfNeeded(exam);
        List<ExamRecord> submitted = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStatus, ExamServiceImpl.RS_SUBMITTED));
        submitted.sort(Comparator
                .comparing((ExamRecord r) -> r.getTotalScore() == null ? BigDecimal.ZERO : r.getTotalScore())
                .reversed()
                .thenComparing(ExamRecord::getStudentId));
        List<StudentRankDTO> rows = new ArrayList<>();
        int rank = 1;
        for (ExamRecord r : submitted) {
            StudentRankDTO dto = new StudentRankDTO();
            dto.setRank(rank++);
            dto.setStudentId(r.getStudentId());
            dto.setTotalScore(r.getTotalScore());
            if (exam.getPassScore() != null && r.getTotalScore() != null) {
                dto.setPassed(r.getTotalScore().compareTo(exam.getPassScore()) >= 0);
            } else {
                dto.setPassed(null);
            }
            User u = userMapper.selectById(r.getStudentId());
            if (u != null) {
                dto.setUsername(u.getUsername());
                dto.setRealName(u.getRealName());
            }
            rows.add(dto);
        }
        Page<StudentRankDTO> out = new Page<>(page, size);
        int from = (int) Math.max(0, (page - 1) * size);
        int to = (int) Math.min(rows.size(), from + (int) size);
        if (from >= rows.size()) {
            out.setRecords(List.of());
            out.setTotal(rows.size());
            return out;
        }
        out.setRecords(rows.subList(from, to));
        out.setTotal(rows.size());
        return out;
    }

    @Override
    public List<ExamQuestionStatDTO> questionStats(Long examId) {
        Exam exam = requireExam(examId);
        coursePermissionService.assertExamReadable(examId);
        assertStudentScorePublishedIfNeeded(exam);
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, exam.getPaperId()));
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStatus, ExamServiceImpl.RS_SUBMITTED));
        List<Long> recordIds = records.stream().map(ExamRecord::getId).toList();
        Map<Long, List<ExamAnswer>> byQ = new HashMap<>();
        if (!recordIds.isEmpty()) {
            List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                    .in(ExamAnswer::getExamRecordId, recordIds));
            byQ = answers.stream().collect(Collectors.groupingBy(ExamAnswer::getQuestionId));
        }
        List<ExamQuestionStatDTO> list = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            ExamQuestionStatDTO dto = new ExamQuestionStatDTO();
            dto.setQuestionId(pq.getQuestionId());
            if (q != null) {
                dto.setType(q.getType());
            }
            List<ExamAnswer> as = byQ.getOrDefault(pq.getQuestionId(), List.of());
            long attempts = as.stream().filter(a -> a.getUserAnswer() != null && !a.getUserAnswer().isBlank()).count();
            long correct = as.stream().filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1).count();
            dto.setAttemptCount(attempts);
            dto.setCorrectCount(correct);
            if (attempts > 0) {
                dto.setCorrectRate(BigDecimal.valueOf(correct)
                        .divide(BigDecimal.valueOf(attempts), 4, RoundingMode.HALF_UP));
            } else {
                dto.setCorrectRate(BigDecimal.ZERO);
            }
            list.add(dto);
        }
        return list;
    }

    @Override
    public List<ExamKnowledgeStatDTO> knowledgePointStats(Long examId) {
        List<ExamQuestionStatDTO> qs = questionStats(examId);
        Map<Long, List<ExamQuestionStatDTO>> byKp = new HashMap<>();
        for (ExamQuestionStatDTO s : qs) {
            Question q = questionMapper.selectById(s.getQuestionId());
            if (q == null) {
                continue;
            }
            byKp.computeIfAbsent(q.getKnowledgePointId(), k -> new ArrayList<>()).add(s);
        }
        List<ExamKnowledgeStatDTO> out = new ArrayList<>();
        for (Map.Entry<Long, List<ExamQuestionStatDTO>> e : byKp.entrySet()) {
            ExamKnowledgeStatDTO dto = new ExamKnowledgeStatDTO();
            dto.setKnowledgePointId(e.getKey());
            KnowledgePoint kp = knowledgePointMapper.selectById(e.getKey());
            if (kp != null) {
                dto.setKnowledgePointName(kp.getName());
            }
            dto.setQuestionCount(e.getValue().size());
            BigDecimal sum = e.getValue().stream()
                    .map(ExamQuestionStatDTO::getCorrectRate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setAvgCorrectRate(sum.divide(BigDecimal.valueOf(e.getValue().size()), 4, RoundingMode.HALF_UP));
            out.add(dto);
        }
        return out;
    }

    @Override
    public void exportRank(Long examId, HttpServletResponse response) throws IOException {
        Page<StudentRankDTO> page = rank(examId, 1, 10_000);
        List<ExamRankExportRow> rows = new ArrayList<>();
        for (StudentRankDTO s : page.getRecords()) {
            ExamRankExportRow r = new ExamRankExportRow();
            r.setRank(s.getRank());
            r.setUsername(s.getUsername());
            r.setRealName(s.getRealName());
            r.setTotalScore(s.getTotalScore());
            rows.add(r);
        }
        String fileName = URLEncoder.encode("exam-" + examId + "-rank.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), ExamRankExportRow.class)
                .sheet("排名")
                .doWrite(rows);
    }
}
