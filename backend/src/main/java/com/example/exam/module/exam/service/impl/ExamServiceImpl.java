package com.example.exam.module.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.QuestionTypeEnum;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.course.entity.Course;
import com.example.exam.module.course.entity.CourseStudent;
import com.example.exam.module.course.mapper.CourseMapper;
import com.example.exam.module.course.mapper.CourseStudentMapper;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.exam.dto.ExamCreateRequest;
import com.example.exam.module.exam.dto.ExamRecordSummaryDTO;
import com.example.exam.module.exam.dto.ExamStartResponse;
import com.example.exam.module.exam.dto.ExamStudentVO;
import com.example.exam.module.exam.dto.SaveAnswersRequest;
import com.example.exam.module.exam.dto.SubmitAnswerRequest;
import com.example.exam.module.exam.dto.TakeQuestionVO;
import com.example.exam.module.exam.entity.Exam;
import com.example.exam.module.exam.entity.ExamAnswer;
import com.example.exam.module.exam.entity.ExamRecord;
import com.example.exam.module.exam.mapper.ExamAnswerMapper;
import com.example.exam.module.exam.mapper.ExamMapper;
import com.example.exam.module.exam.mapper.ExamRecordMapper;
import com.example.exam.module.exam.service.ExamService;
import com.example.exam.module.exam.util.ExamShuffleHelper;
import com.example.exam.module.exam.util.QuestionAnswerMatcher;
import com.example.exam.module.paper.entity.PaperQuestion;
import com.example.exam.module.paper.mapper.PaperQuestionMapper;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.mapper.QuestionMapper;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import com.example.exam.module.wrongbook.service.WrongBookService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    public static final String ST_DRAFT = "DRAFT";
    public static final String ST_PUBLISHED = "PUBLISHED";
    public static final String ST_ENDED = "ENDED";

    public static final String RS_IN_PROGRESS = "IN_PROGRESS";
    public static final String RS_SUBMITTED = "SUBMITTED";
    public static final String RS_EXPIRED = "EXPIRED";

    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final CoursePermissionService coursePermissionService;
    private final WrongBookService wrongBookService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private User me() {
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return u;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Exam create(ExamCreateRequest req) {
        coursePermissionService.assertCourseManageable(req.getCourseId());
        User u = me();
        Exam e = new Exam();
        e.setCourseId(req.getCourseId());
        e.setPaperId(req.getPaperId());
        e.setTitle(req.getTitle());
        e.setDescription(req.getDescription());
        e.setStartTime(req.getStartTime());
        e.setEndTime(req.getEndTime());
        e.setDurationMinutes(req.getDurationMinutes());
        applyExamOptionalFields(e, req);
        e.setStatus(ST_DRAFT);
        e.setCreatorId(u.getId());
        e.setCreateTime(LocalDateTime.now());
        e.setUpdateTime(LocalDateTime.now());
        examMapper.insert(e);
        return e;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Exam update(Long id, ExamCreateRequest req) {
        Exam e = examMapper.selectById(id);
        if (e == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试不存在");
        }
        if (!ST_DRAFT.equals(e.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿可编辑");
        }
        coursePermissionService.assertCourseManageable(e.getCourseId());
        e.setCourseId(req.getCourseId());
        e.setPaperId(req.getPaperId());
        e.setTitle(req.getTitle());
        e.setDescription(req.getDescription());
        e.setStartTime(req.getStartTime());
        e.setEndTime(req.getEndTime());
        e.setDurationMinutes(req.getDurationMinutes());
        applyExamOptionalFields(e, req);
        e.setUpdateTime(LocalDateTime.now());
        examMapper.updateById(e);
        return e;
    }

    private void applyExamOptionalFields(Exam e, ExamCreateRequest req) {
        e.setPassScore(req.getPassScore());
        if (req.getScorePublished() != null) {
            e.setScorePublished(req.getScorePublished() ? 1 : 0);
        } else if (e.getScorePublished() == null) {
            e.setScorePublished(0);
        }
        if (req.getShuffleQuestions() != null) {
            e.setShuffleQuestions(req.getShuffleQuestions() ? 1 : 0);
        } else if (e.getShuffleQuestions() == null) {
            e.setShuffleQuestions(1);
        }
        if (req.getShuffleOptions() != null) {
            e.setShuffleOptions(req.getShuffleOptions() ? 1 : 0);
        } else if (e.getShuffleOptions() == null) {
            e.setShuffleOptions(1);
        }
        if (req.getSwitchBlurLimit() != null) {
            e.setSwitchBlurLimit(req.getSwitchBlurLimit());
        }
    }

    private boolean shuffleQuestionsOn(Exam exam) {
        return exam.getShuffleQuestions() == null || exam.getShuffleQuestions() != 0;
    }

    private boolean shuffleOptionsOn(Exam exam) {
        return exam.getShuffleOptions() == null || exam.getShuffleOptions() != 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        Exam e = requireExam(id);
        coursePermissionService.assertCourseManageable(e.getCourseId());
        if (!ST_DRAFT.equals(e.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仅草稿可发布");
        }
        e.setStatus(ST_PUBLISHED);
        e.setUpdateTime(LocalDateTime.now());
        examMapper.updateById(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endExam(Long id) {
        Exam e = requireExam(id);
        coursePermissionService.assertCourseManageable(e.getCourseId());
        e.setStatus(ST_ENDED);
        e.setUpdateTime(LocalDateTime.now());
        examMapper.updateById(e);
    }

    @Override
    public Page<Exam> pageForTeacher(Long courseId, long page, long size) {
        coursePermissionService.assertCourseManageable(courseId);
        LambdaQueryWrapper<Exam> w = new LambdaQueryWrapper<Exam>().eq(Exam::getCourseId, courseId);
        Page<Exam> p = new Page<>(page, size);
        examMapper.selectPage(p, w.orderByDesc(Exam::getCreateTime));
        return p;
    }

    @Override
    public Page<ExamStudentVO> pageForStudent(long page, long size, String keyword) {
        User u = me();
        boolean admin = RoleEnum.ADMIN.name().equals(u.getRole());
        boolean student = RoleEnum.STUDENT.name().equals(u.getRole());
        if (!student && !admin) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅学生或管理员可查看");
        }
        LambdaQueryWrapper<Exam> w = new LambdaQueryWrapper<Exam>()
                .eq(Exam::getStatus, ST_PUBLISHED);
        if (!admin) {
            List<Long> courseIds = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                            .eq(CourseStudent::getStudentId, u.getId())
                            .eq(CourseStudent::getStatus, 1)).stream()
                    .map(CourseStudent::getCourseId)
                    .distinct()
                    .toList();
            if (courseIds.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            w.in(Exam::getCourseId, courseIds);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.like(Exam::getTitle, keyword);
        }
        Page<Exam> src = new Page<>(page, size);
        examMapper.selectPage(src, w.orderByDesc(Exam::getStartTime));
        Page<ExamStudentVO> out = new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        List<ExamStudentVO> vos = new ArrayList<>();
        for (Exam e : src.getRecords()) {
            ExamStudentVO vo = new ExamStudentVO();
            vo.setId(e.getId());
            vo.setCourseId(e.getCourseId());
            vo.setTitle(e.getTitle());
            vo.setStartTime(e.getStartTime());
            vo.setEndTime(e.getEndTime());
            vo.setDurationMinutes(e.getDurationMinutes());
            vo.setStatus(e.getStatus());
            Course c = courseMapper.selectById(e.getCourseId());
            if (c != null) {
                vo.setCourseName(c.getName());
            }
            vos.add(vo);
        }
        out.setRecords(vos);
        return out;
    }

    @Override
    public Exam get(Long id) {
        Exam e = requireExam(id);
        coursePermissionService.assertExamReadable(id);
        return e;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamStartResponse startExam(Long examId) {
        User u = me();
        if (!RoleEnum.STUDENT.name().equals(u.getRole()) && !RoleEnum.ADMIN.name().equals(u.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅学生或管理员可参加考试");
        }
        Exam exam = requireExam(examId);
        coursePermissionService.assertExamReadable(examId);
        if (!ST_PUBLISHED.equals(exam.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "考试未发布或已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime())) {
            throw new BizException(ErrorCode.CONFLICT, "不在考试时间范围内");
        }
        ExamRecord record = new ExamRecord();
        record.setExamId(examId);
        record.setStudentId(u.getId());
        record.setStatus(RS_IN_PROGRESS);
        record.setStartedAt(now);
        record.setSwitchBlurCount(0);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        boolean insertedFresh;
        try {
            examRecordMapper.insert(record);
            insertedFresh = true;
        } catch (DataIntegrityViolationException ex) {
            insertedFresh = false;
            record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, examId)
                    .eq(ExamRecord::getStudentId, u.getId()));
            if (record == null) {
                throw new BizException(ErrorCode.CONFLICT, "无法开始考试，请重试");
            }
        }
        if (RS_SUBMITTED.equals(record.getStatus()) || RS_EXPIRED.equals(record.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "考试已交卷或已过期");
        }
        ensureExamAnswerRows(record.getId());
        // 新会话或异常中断后未写入乱序种子时，才生成乱序；断线重连保持同一套顺序
        if (insertedFresh || record.getShuffleSeed() == null) {
            long shuffleSeed = ThreadLocalRandom.current().nextLong();
            record.setShuffleSeed(shuffleSeed);
            if (shuffleQuestionsOn(exam)) {
                List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                        .eq(PaperQuestion::getPaperId, exam.getPaperId())
                        .orderByAsc(PaperQuestion::getQuestionOrder));
                List<Long> baseOrder = pqs.stream().map(PaperQuestion::getQuestionId).toList();
                try {
                    record.setQuestionOrderJson(objectMapper.writeValueAsString(
                            ExamShuffleHelper.shuffledQuestionIds(baseOrder, shuffleSeed)));
                } catch (Exception ex) {
                    throw new BizException(ErrorCode.INTERNAL_ERROR, "题目顺序生成失败");
                }
            } else {
                record.setQuestionOrderJson(null);
            }
            record.setUpdateTime(LocalDateTime.now());
            examRecordMapper.updateById(record);
        }
        ExamStartResponse resp = new ExamStartResponse();
        resp.setRecordId(record.getId());
        resp.setExamId(examId);
        resp.setStartedAt(record.getStartedAt());
        resp.setDurationMinutes(exam.getDurationMinutes());
        LocalDateTime deadline = record.getStartedAt().plusMinutes(exam.getDurationMinutes());
        if (exam.getEndTime().isBefore(deadline)) {
            deadline = exam.getEndTime();
        }
        resp.setDeadlineAt(deadline);
        resp.setShuffleQuestions(shuffleQuestionsOn(exam));
        resp.setShuffleOptions(shuffleOptionsOn(exam));
        resp.setSwitchBlurLimit(exam.getSwitchBlurLimit());
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureExamAnswerRows(Long examRecordId) {
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试记录不存在");
        }
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam == null) {
            return;
        }
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, exam.getPaperId())
                .orderByAsc(PaperQuestion::getQuestionOrder));
        for (PaperQuestion pq : pqs) {
            ExamAnswer exist = examAnswerMapper.selectOne(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getExamRecordId, examRecordId)
                    .eq(ExamAnswer::getQuestionId, pq.getQuestionId()));
            if (exist != null) {
                continue;
            }
            ExamAnswer a = new ExamAnswer();
            a.setExamRecordId(examRecordId);
            a.setQuestionId(pq.getQuestionId());
            a.setCreateTime(LocalDateTime.now());
            a.setUpdateTime(LocalDateTime.now());
            examAnswerMapper.insert(a);
        }
    }

    @Override
    public List<TakeQuestionVO> listTakeQuestions(Long examRecordId) {
        User u = me();
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试记录不存在");
        }
        if (!record.getStudentId().equals(u.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该答卷");
        }
        coursePermissionService.assertExamReadable(record.getExamId());
        Exam exam = examMapper.selectById(record.getExamId());
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, exam.getPaperId())
                .orderByAsc(PaperQuestion::getQuestionOrder));
        Map<Long, PaperQuestion> pqByQid = pqs.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, x -> x, (a, b) -> a));
        List<Long> questionIds;
        try {
            if (record.getQuestionOrderJson() != null && !record.getQuestionOrderJson().isBlank()) {
                questionIds = objectMapper.readValue(record.getQuestionOrderJson(), new TypeReference<>() {
                });
            } else {
                questionIds = pqs.stream().map(PaperQuestion::getQuestionId).toList();
            }
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "题目顺序解析失败");
        }
        Map<Long, ExamAnswer> ansMap = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getExamRecordId, examRecordId)).stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, x -> x, (a, b) -> a));
        List<TakeQuestionVO> list = new ArrayList<>();
        int displayOrder = 1;
        for (Long qid : questionIds) {
            PaperQuestion pq = pqByQid.get(qid);
            if (pq == null) {
                continue;
            }
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            TakeQuestionVO vo = new TakeQuestionVO();
            vo.setQuestionId(q.getId());
            vo.setOrderNo(displayOrder++);
            vo.setType(q.getType());
            vo.setStem(q.getStem());
            String optJson = q.getOptionsJson();
            if (shuffleOptionsOn(exam) && record.getShuffleSeed() != null
                    && ExamShuffleHelper.supportsOptionShuffle(q.getType())) {
                optJson = ExamShuffleHelper.shuffleOptionsJson(optJson, record.getShuffleSeed(), q.getId(), objectMapper);
            }
            vo.setOptionsJson(optJson);
            vo.setScore(pq.getScore());
            ExamAnswer ea = ansMap.get(q.getId());
            if (ea != null) {
                String ua = ea.getUserAnswer();
                if (ua != null && shuffleOptionsOn(exam) && record.getShuffleSeed() != null
                        && ExamShuffleHelper.supportsOptionShuffle(q.getType())) {
                    ua = ExamShuffleHelper.originalAnswerToDisplay(q.getOptionsJson(), record.getShuffleSeed(),
                            q.getId(), ua, objectMapper);
                }
                vo.setUserAnswer(ua);
            }
            list.add(vo);
        }
        return list;
    }

    @Override
    public ExamRecordSummaryDTO getRecordSummary(Long examRecordId) {
        User u = me();
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试记录不存在");
        }
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试不存在");
        }
        if (RoleEnum.STUDENT.name().equals(u.getRole())) {
            if (!record.getStudentId().equals(u.getId())) {
                throw new BizException(ErrorCode.FORBIDDEN, "无权查看该答卷");
            }
        } else {
            coursePermissionService.assertExamReadable(record.getExamId());
        }
        ExamRecordSummaryDTO dto = new ExamRecordSummaryDTO();
        dto.setRecordId(examRecordId);
        dto.setExamId(exam.getId());
        dto.setExamTitle(exam.getTitle());
        dto.setCourseId(exam.getCourseId());
        dto.setRecordStatus(record.getStatus());
        dto.setTotalScore(record.getTotalScore());
        dto.setPassScore(exam.getPassScore());
        dto.setScorePublished(exam.getScorePublished() != null && exam.getScorePublished() != 0);
        dto.setSwitchBlurCount(record.getSwitchBlurCount());
        dto.setSwitchBlurLimit(exam.getSwitchBlurLimit());
        dto.setSubmittedAt(record.getSubmittedAt());
        dto.setStartedAt(record.getStartedAt());
        if (record.getStartedAt() != null) {
            LocalDateTime deadline = record.getStartedAt().plusMinutes(exam.getDurationMinutes());
            if (exam.getEndTime() != null && exam.getEndTime().isBefore(deadline)) {
                deadline = exam.getEndTime();
            }
            dto.setDeadlineAt(deadline);
        }
        if (exam.getPassScore() != null && record.getTotalScore() != null) {
            dto.setPassed(record.getTotalScore().compareTo(exam.getPassScore()) >= 0);
        } else {
            dto.setPassed(null);
        }
        if (RS_SUBMITTED.equals(record.getStatus())) {
            List<ExamRecord> submitted = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, exam.getId())
                    .eq(ExamRecord::getStatus, RS_SUBMITTED));
            submitted.sort(Comparator
                    .comparing((ExamRecord r) -> r.getTotalScore() == null ? BigDecimal.ZERO : r.getTotalScore())
                    .reversed()
                    .thenComparing(ExamRecord::getStudentId));
            dto.setRankTotal(submitted.size());
            boolean showRank = !RoleEnum.STUDENT.name().equals(u.getRole())
                    || Boolean.TRUE.equals(dto.getScorePublished());
            if (showRank) {
                int rank = 1;
                for (ExamRecord r : submitted) {
                    if (r.getId().equals(examRecordId)) {
                        dto.setRank(rank);
                        break;
                    }
                    rank++;
                }
            }
        } else {
            dto.setRank(null);
            dto.setRankTotal(null);
        }
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswers(Long examRecordId, SaveAnswersRequest req) {
        User u = me();
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试记录不存在");
        }
        if (!record.getStudentId().equals(u.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作");
        }
        if (!RS_IN_PROGRESS.equals(record.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仅进行中的考试可保存");
        }
        coursePermissionService.assertExamReadable(record.getExamId());
        if (req.getAnswers() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "answers 不能为空");
        }
        List<SubmitAnswerRequest.AnswerItem> mapped = new ArrayList<>();
        for (SaveAnswersRequest.AnswerItem a : req.getAnswers()) {
            SubmitAnswerRequest.AnswerItem i = new SubmitAnswerRequest.AnswerItem();
            i.setQuestionId(a.getQuestionId());
            i.setUserAnswer(a.getUserAnswer());
            mapped.add(i);
        }
        patchAnswers(record, mapped, false, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAnswers(Long examRecordId, SubmitAnswerRequest req) {
        User u = me();
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试记录不存在");
        }
        if (!record.getStudentId().equals(u.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作");
        }
        coursePermissionService.assertExamReadable(record.getExamId());
        submitAnswersInternal(record, req, true);
    }

    /**
     * @param convertDisplayAnswer 见 {@link #patchAnswers}
     */
    private void submitAnswersInternal(ExamRecord record, SubmitAnswerRequest req, boolean convertDisplayAnswer) {
        if (RS_SUBMITTED.equals(record.getStatus())) {
            return;
        }
        if (!RS_IN_PROGRESS.equals(record.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "当前状态不可交卷");
        }
        coursePermissionService.assertExamReadable(record.getExamId());
        patchAnswers(record, req.getAnswers(), true, convertDisplayAnswer);
        LocalDateTime now = LocalDateTime.now();
        gradeAndFinalizeRecord(record.getId());
        record.setStatus(RS_SUBMITTED);
        record.setSubmittedAt(now);
        record.setUpdateTime(now);
        examRecordMapper.updateById(record);
        wrongBookService.syncAfterExamSubmit(record.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setScorePublished(Long examId, boolean published) {
        Exam e = requireExam(examId);
        coursePermissionService.assertExamManageable(examId);
        e.setScorePublished(published ? 1 : 0);
        e.setUpdateTime(LocalDateTime.now());
        examMapper.updateById(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportSwitchBlur(Long examRecordId) {
        User u = me();
        if (!RoleEnum.STUDENT.name().equals(u.getRole()) && !RoleEnum.ADMIN.name().equals(u.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅学生或管理员可上报");
        }
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试记录不存在");
        }
        if (!record.getStudentId().equals(u.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作");
        }
        if (!RS_IN_PROGRESS.equals(record.getStatus())) {
            return;
        }
        coursePermissionService.assertExamReadable(record.getExamId());
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam == null) {
            return;
        }
        int c = record.getSwitchBlurCount() == null ? 0 : record.getSwitchBlurCount();
        c++;
        record.setSwitchBlurCount(c);
        record.setUpdateTime(LocalDateTime.now());
        examRecordMapper.updateById(record);
        Integer limit = exam.getSwitchBlurLimit();
        if (limit != null && limit > 0 && c >= limit) {
            List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getExamRecordId, examRecordId));
            SubmitAnswerRequest req = new SubmitAnswerRequest();
            List<SubmitAnswerRequest.AnswerItem> items = new ArrayList<>();
            for (ExamAnswer a : answers) {
                SubmitAnswerRequest.AnswerItem it = new SubmitAnswerRequest.AnswerItem();
                it.setQuestionId(a.getQuestionId());
                it.setUserAnswer(a.getUserAnswer());
                items.add(it);
            }
            req.setAnswers(items);
            ExamRecord again = examRecordMapper.selectById(examRecordId);
            submitAnswersInternal(again, req, false);
        }
    }

    /**
     * @param convertDisplayAnswer true：客户端提交的是乱序后的展示标号；false：已是库内原始标号（如从 DB 读出再提交）
     */
    private void patchAnswers(ExamRecord record, List<SubmitAnswerRequest.AnswerItem> items, boolean gradeObjectives,
                              boolean convertDisplayAnswer) {
        Exam exam = examMapper.selectById(record.getExamId());
        Map<Long, PaperQuestion> pqMap = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                        .eq(PaperQuestion::getPaperId, exam.getPaperId())).stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, x -> x, (a, b) -> a));
        Map<Long, ExamAnswer> ansMap = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getExamRecordId, record.getId())).stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, x -> x, (a, b) -> a));
        for (SubmitAnswerRequest.AnswerItem it : items) {
            PaperQuestion pq = pqMap.get(it.getQuestionId());
            if (pq == null) {
                continue;
            }
            Question q = questionMapper.selectById(it.getQuestionId());
            if (q == null) {
                continue;
            }
            String ua = it.getUserAnswer();
            if (convertDisplayAnswer && ua != null && shuffleOptionsOn(exam) && record.getShuffleSeed() != null
                    && ExamShuffleHelper.supportsOptionShuffle(q.getType())) {
                ua = ExamShuffleHelper.displayAnswerToOriginal(q.getOptionsJson(), record.getShuffleSeed(),
                        q.getId(), ua, objectMapper);
            }
            ExamAnswer a = ansMap.get(it.getQuestionId());
            if (a == null) {
                a = new ExamAnswer();
                a.setExamRecordId(record.getId());
                a.setQuestionId(it.getQuestionId());
                a.setCreateTime(LocalDateTime.now());
                a.setUpdateTime(LocalDateTime.now());
                examAnswerMapper.insert(a);
                ansMap.put(it.getQuestionId(), a);
            }
            a.setUserAnswer(ua);
            a.setUpdateTime(LocalDateTime.now());
            if (gradeObjectives && !QuestionTypeEnum.SHORT.name().equals(q.getType())) {
                boolean ok = QuestionAnswerMatcher.matches(q.getType(), q.getAnswer(), ua);
                a.setScore(ok ? pq.getScore() : BigDecimal.ZERO);
                a.setIsCorrect(ok ? 1 : 0);
                a.setGradedAt(LocalDateTime.now());
            }
            examAnswerMapper.updateById(a);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoSubmitExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamRecord> list = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStatus, RS_IN_PROGRESS));
        int n = 0;
        for (ExamRecord r : list) {
            Exam exam = examMapper.selectById(r.getExamId());
            if (exam == null) {
                continue;
            }
            LocalDateTime deadline = r.getStartedAt() != null
                    ? r.getStartedAt().plusMinutes(exam.getDurationMinutes())
                    : exam.getEndTime();
            if (exam.getEndTime().isBefore(deadline)) {
                deadline = exam.getEndTime();
            }
            if (now.isAfter(deadline)) {
                r.setStatus(RS_SUBMITTED);
                r.setSubmittedAt(deadline);
                r.setUpdateTime(now);
                examRecordMapper.updateById(r);
                List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getExamRecordId, r.getId()));
                Map<Long, PaperQuestion> pqMap = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                                .eq(PaperQuestion::getPaperId, exam.getPaperId())).stream()
                        .collect(Collectors.toMap(PaperQuestion::getQuestionId, x -> x, (a, b) -> a));
                for (ExamAnswer a : answers) {
                    Question q = questionMapper.selectById(a.getQuestionId());
                    PaperQuestion pq = pqMap.get(a.getQuestionId());
                    if (q == null || pq == null) {
                        continue;
                    }
                    if (!QuestionTypeEnum.SHORT.name().equals(q.getType())) {
                        boolean ok = QuestionAnswerMatcher.matches(q.getType(), q.getAnswer(), a.getUserAnswer());
                        a.setScore(ok ? pq.getScore() : BigDecimal.ZERO);
                        a.setIsCorrect(ok ? 1 : 0);
                        a.setGradedAt(now);
                        examAnswerMapper.updateById(a);
                    }
                }
                wrongBookService.syncAfterExamSubmit(r.getId());
                gradeAndFinalizeRecord(r.getId());
                n++;
            }
        }
        return n;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeAndFinalizeRecord(Long examRecordId) {
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试记录不存在");
        }
        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getExamRecordId, examRecordId));
        BigDecimal total = BigDecimal.ZERO;
        for (ExamAnswer a : answers) {
            if (a.getScore() != null) {
                total = total.add(a.getScore());
            }
        }
        record.setTotalScore(total);
        record.setUpdateTime(LocalDateTime.now());
        examRecordMapper.updateById(record);
    }

    private Exam requireExam(Long id) {
        Exam e = examMapper.selectById(id);
        if (e == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试不存在");
        }
        return e;
    }
}
