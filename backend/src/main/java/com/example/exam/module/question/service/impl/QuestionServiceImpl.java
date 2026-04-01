package com.example.exam.module.question.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.QuestionReviewStatusEnum;
import com.example.exam.common.enums.QuestionTypeEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.question.dto.QuestionBatchUpdateRequest;
import com.example.exam.module.question.dto.QuestionCreateRequest;
import com.example.exam.module.question.dto.QuestionDedupCheckRequest;
import com.example.exam.module.question.dto.QuestionDedupHitVO;
import com.example.exam.module.question.dto.QuestionDedupResultVO;
import com.example.exam.module.question.dto.QuestionExcelRow;
import com.example.exam.module.question.dto.QuestionImportResultVO;
import com.example.exam.module.question.dto.QuestionSaveRequest;
import com.example.exam.module.question.dto.QuestionUpdateRequest;
import com.example.exam.module.question.dto.QuestionVersionVO;
import com.example.exam.module.question.entity.KnowledgePoint;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.entity.QuestionVersion;
import com.example.exam.module.question.mapper.KnowledgePointMapper;
import com.example.exam.module.question.mapper.QuestionMapper;
import com.example.exam.module.question.mapper.QuestionVersionMapper;
import com.example.exam.module.question.support.QuestionSimilarityHelper;
import com.example.exam.module.question.service.QuestionService;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionVersionMapper questionVersionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final CoursePermissionService coursePermissionService;
    private final UserService userService;

    private static final int DEDUP_MAX_COMPARE = 5000;

    private int currentVersionNo(Question q) {
        return q.getVersionNo() == null ? 1 : q.getVersionNo();
    }

    /**
     * 将当前行内容归档为历史版本，并把题目 version_no +1（调用方随后再写回 question 表）。
     */
    private void archiveCurrentAsHistory(Question q) {
        QuestionVersion v = new QuestionVersion();
        v.setQuestionId(q.getId());
        int vn = currentVersionNo(q);
        v.setVersionNo(vn);
        v.setKnowledgePointId(q.getKnowledgePointId());
        v.setType(q.getType());
        v.setStem(q.getStem());
        v.setOptionsJson(q.getOptionsJson());
        v.setAnswer(q.getAnswer());
        v.setAnalysis(q.getAnalysis());
        v.setScoreDefault(q.getScoreDefault());
        v.setDifficulty(q.getDifficulty());
        v.setStatus(q.getStatus());
        v.setReviewStatus(q.getReviewStatus());
        v.setEditorId(me().getId());
        v.setCreateTime(LocalDateTime.now());
        questionVersionMapper.insert(v);
        q.setVersionNo(vn + 1);
    }

    private boolean willChange(Question old, QuestionUpdateRequest req) {
        if (req.getKnowledgePointId() != null && !req.getKnowledgePointId().equals(old.getKnowledgePointId())) {
            return true;
        }
        if (req.getType() != null && !req.getType().equals(old.getType())) {
            return true;
        }
        if (req.getStem() != null && !req.getStem().equals(old.getStem())) {
            return true;
        }
        if (req.getOptionsJson() != null && !Objects.equals(req.getOptionsJson(), old.getOptionsJson())) {
            return true;
        }
        if (req.getAnswer() != null && !req.getAnswer().equals(old.getAnswer())) {
            return true;
        }
        if (req.getAnalysis() != null && !Objects.equals(req.getAnalysis(), old.getAnalysis())) {
            return true;
        }
        if (req.getScoreDefault() != null && old.getScoreDefault().compareTo(req.getScoreDefault()) != 0) {
            return true;
        }
        if (req.getDifficulty() != null && !req.getDifficulty().equals(old.getDifficulty())) {
            return true;
        }
        if (req.getStatus() != null && !req.getStatus().equals(old.getStatus())) {
            return true;
        }
        if (req.getReviewStatus() != null && !req.getReviewStatus().equals(old.getReviewStatus())) {
            return true;
        }
        return false;
    }

    private QuestionVersionVO toVersionVo(QuestionVersion v) {
        QuestionVersionVO vo = new QuestionVersionVO();
        vo.setId(v.getId());
        vo.setVersionNo(v.getVersionNo());
        vo.setKnowledgePointId(v.getKnowledgePointId());
        vo.setType(v.getType());
        vo.setStem(v.getStem());
        vo.setOptionsJson(v.getOptionsJson());
        vo.setAnswer(v.getAnswer());
        vo.setAnalysis(v.getAnalysis());
        vo.setScoreDefault(v.getScoreDefault());
        vo.setDifficulty(v.getDifficulty());
        vo.setStatus(v.getStatus());
        vo.setReviewStatus(v.getReviewStatus());
        vo.setEditorId(v.getEditorId());
        vo.setCreateTime(v.getCreateTime());
        return vo;
    }

    private void assertType(String type) {
        if (type == null || !QuestionTypeEnum.isValid(type)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "题型不合法");
        }
    }

    private void assertReviewStatus(String rs) {
        if (rs == null || !QuestionReviewStatusEnum.isValid(rs)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "审核状态不合法");
        }
    }

    private User me() {
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return u;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Question create(QuestionCreateRequest req) {
        assertType(req.getType());
        coursePermissionService.assertCourseManageable(req.getCourseId());
        KnowledgePoint kp = knowledgePointMapper.selectById(req.getKnowledgePointId());
        if (kp == null || !kp.getCourseId().equals(req.getCourseId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "知识点不存在或不属于该课程");
        }
        Question q = new Question();
        q.setCourseId(req.getCourseId());
        q.setKnowledgePointId(req.getKnowledgePointId());
        q.setType(req.getType());
        q.setStem(req.getStem());
        q.setOptionsJson(req.getOptionsJson());
        q.setAnswer(req.getAnswer());
        q.setAnalysis(req.getAnalysis());
        q.setScoreDefault(req.getScoreDefault() != null ? req.getScoreDefault() : BigDecimal.TEN);
        q.setDifficulty(req.getDifficulty() != null ? req.getDifficulty() : 1);
        q.setCreatorId(me().getId());
        q.setStatus(1);
        if (req.getReviewStatus() != null) {
            assertReviewStatus(req.getReviewStatus());
            q.setReviewStatus(req.getReviewStatus());
        } else {
            q.setReviewStatus(QuestionReviewStatusEnum.PUBLISHED.name());
        }
        q.setVersionNo(1);
        q.setCreateTime(LocalDateTime.now());
        q.setUpdateTime(LocalDateTime.now());
        questionMapper.insert(q);
        return q;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Question update(Long id, QuestionUpdateRequest req) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试题不存在");
        }
        coursePermissionService.assertCourseManageable(q.getCourseId());
        if (willChange(q, req)) {
            archiveCurrentAsHistory(q);
        }
        if (req.getKnowledgePointId() != null) {
            KnowledgePoint kp = knowledgePointMapper.selectById(req.getKnowledgePointId());
            if (kp == null || !kp.getCourseId().equals(q.getCourseId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "知识点不存在或不属于该课程");
            }
            q.setKnowledgePointId(req.getKnowledgePointId());
        }
        if (req.getType() != null) {
            assertType(req.getType());
            q.setType(req.getType());
        }
        if (req.getStem() != null) {
            q.setStem(req.getStem());
        }
        if (req.getOptionsJson() != null) {
            q.setOptionsJson(req.getOptionsJson());
        }
        if (req.getAnswer() != null) {
            q.setAnswer(req.getAnswer());
        }
        if (req.getAnalysis() != null) {
            q.setAnalysis(req.getAnalysis());
        }
        if (req.getScoreDefault() != null) {
            q.setScoreDefault(req.getScoreDefault());
        }
        if (req.getDifficulty() != null) {
            q.setDifficulty(req.getDifficulty());
        }
        if (req.getStatus() != null) {
            q.setStatus(req.getStatus());
        }
        if (req.getReviewStatus() != null) {
            assertReviewStatus(req.getReviewStatus());
            q.setReviewStatus(req.getReviewStatus());
        }
        q.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(q);
        return q;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Question save(QuestionSaveRequest req) {
        if (req.getType() != null) {
            assertType(req.getType());
        }
        coursePermissionService.assertCourseManageable(req.getCourseId());
        if (req.getId() == null) {
            QuestionCreateRequest c = new QuestionCreateRequest();
            c.setCourseId(req.getCourseId());
            c.setKnowledgePointId(req.getKnowledgePointId());
            c.setType(req.getType() != null ? req.getType() : QuestionTypeEnum.SINGLE.name());
            c.setStem(req.getStem() != null ? req.getStem() : "");
            c.setOptionsJson(req.getOptionsJson());
            c.setAnswer(req.getAnswer() != null ? req.getAnswer() : "");
            c.setAnalysis(req.getAnalysis());
            c.setScoreDefault(req.getScoreDefault());
            c.setDifficulty(req.getDifficulty());
            c.setReviewStatus(req.getReviewStatus());
            return create(c);
        }
        Question existing = questionMapper.selectById(req.getId());
        if (existing == null || !existing.getCourseId().equals(req.getCourseId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "试题不存在");
        }
        QuestionUpdateRequest u = new QuestionUpdateRequest();
        u.setKnowledgePointId(req.getKnowledgePointId());
        u.setType(req.getType());
        u.setStem(req.getStem());
        u.setOptionsJson(req.getOptionsJson());
        u.setAnswer(req.getAnswer());
        u.setAnalysis(req.getAnalysis());
        u.setScoreDefault(req.getScoreDefault());
        u.setDifficulty(req.getDifficulty());
        u.setStatus(req.getStatus());
        u.setReviewStatus(req.getReviewStatus());
        return update(req.getId(), u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            return;
        }
        coursePermissionService.assertCourseManageable(q.getCourseId());
        questionMapper.deleteById(id);
    }

    @Override
    public Question get(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试题不存在");
        }
        coursePermissionService.assertCourseReadable(q.getCourseId());
        return q;
    }

    @Override
    public Page<Question> page(Long courseId, Long knowledgePointId, String type, String keyword,
                                 String reviewStatus, long page, long size) {
        coursePermissionService.assertCourseReadable(courseId);
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId);
        if (knowledgePointId != null) {
            w.eq(Question::getKnowledgePointId, knowledgePointId);
        }
        if (type != null && !type.isBlank()) {
            w.eq(Question::getType, type);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.like(Question::getStem, keyword);
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            w.eq(Question::getReviewStatus, reviewStatus);
        }
        Page<Question> p = new Page<>(page, size);
        questionMapper.selectPage(p, w.orderByDesc(Question::getUpdateTime));
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdate(QuestionBatchUpdateRequest req) {
        coursePermissionService.assertCourseManageable(req.getCourseId());
        int n = 0;
        for (Long id : req.getQuestionIds()) {
            Question q = questionMapper.selectById(id);
            if (q == null || !q.getCourseId().equals(req.getCourseId())) {
                continue;
            }
            if (req.getKnowledgePointId() != null) {
                KnowledgePoint kp = knowledgePointMapper.selectById(req.getKnowledgePointId());
                if (kp == null || !kp.getCourseId().equals(req.getCourseId())) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "知识点不存在或不属于该课程");
                }
            }
            boolean hasChange = req.getKnowledgePointId() != null || req.getDifficulty() != null;
            if (!hasChange) {
                continue;
            }
            archiveCurrentAsHistory(q);
            if (req.getKnowledgePointId() != null) {
                q.setKnowledgePointId(req.getKnowledgePointId());
            }
            if (req.getDifficulty() != null) {
                q.setDifficulty(req.getDifficulty());
            }
            q.setUpdateTime(LocalDateTime.now());
            questionMapper.updateById(q);
            n++;
        }
        return n;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionImportResultVO importExcel(Long courseId, MultipartFile file) {
        coursePermissionService.assertCourseManageable(courseId);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请上传 Excel 文件");
        }
        List<QuestionExcelRow> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), QuestionExcelRow.class, new ReadListener<QuestionExcelRow>() {
                @Override
                public void invoke(QuestionExcelRow data, AnalysisContext analysisContext) {
                    rows.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Excel 解析失败: " + e.getMessage());
        }

        List<String> errors = new ArrayList<>();
        int ok = 0;
        int fail = 0;
        for (int i = 0; i < rows.size(); i++) {
            QuestionExcelRow row = rows.get(i);
            int excelRow = i + 2;
            if (row == null || (row.getStem() == null || row.getStem().isBlank())) {
                continue;
            }
            try {
                if (row.getType() == null || row.getType().isBlank()) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "题型不能为空");
                }
                assertType(row.getType().trim());
                if (row.getKnowledgePointId() == null) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "知识点ID不能为空");
                }
                KnowledgePoint kp = knowledgePointMapper.selectById(row.getKnowledgePointId());
                if (kp == null || !kp.getCourseId().equals(courseId)) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "知识点无效");
                }
                String rs = row.getReviewStatus();
                if (rs != null && !rs.isBlank()) {
                    assertReviewStatus(rs.trim());
                }
                Question q = new Question();
                q.setCourseId(courseId);
                q.setKnowledgePointId(row.getKnowledgePointId());
                q.setType(row.getType().trim());
                q.setStem(row.getStem().trim());
                q.setOptionsJson(row.getOptionsJson());
                q.setAnswer(row.getAnswer() != null ? row.getAnswer().trim() : "");
                q.setAnalysis(row.getAnalysis());
                q.setScoreDefault(row.getScoreDefault() != null ? row.getScoreDefault() : BigDecimal.TEN);
                q.setDifficulty(row.getDifficulty() != null ? row.getDifficulty() : 1);
                q.setCreatorId(me().getId());
                q.setStatus(1);
                if (rs != null && !rs.isBlank()) {
                    q.setReviewStatus(rs.trim());
                } else {
                    q.setReviewStatus(QuestionReviewStatusEnum.PUBLISHED.name());
                }
                q.setVersionNo(1);
                q.setCreateTime(LocalDateTime.now());
                q.setUpdateTime(LocalDateTime.now());
                questionMapper.insert(q);
                ok++;
            } catch (BizException ex) {
                fail++;
                errors.add("第" + excelRow + "行: " + ex.getMessage());
            } catch (Exception ex) {
                fail++;
                errors.add("第" + excelRow + "行: " + ex.getMessage());
            }
        }
        return QuestionImportResultVO.builder().success(ok).failed(fail).errors(errors).build();
    }

    @Override
    public void exportExcel(Long courseId, HttpServletResponse response) throws IOException {
        coursePermissionService.assertCourseReadable(courseId);
        List<Question> list = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .orderByAsc(Question::getId));
        List<QuestionExcelRow> rows = new ArrayList<>();
        for (Question q : list) {
            QuestionExcelRow r = new QuestionExcelRow();
            r.setId(q.getId());
            r.setType(q.getType());
            r.setKnowledgePointId(q.getKnowledgePointId());
            r.setStem(q.getStem());
            r.setOptionsJson(q.getOptionsJson());
            r.setAnswer(q.getAnswer());
            r.setAnalysis(q.getAnalysis());
            r.setDifficulty(q.getDifficulty());
            r.setScoreDefault(q.getScoreDefault());
            r.setReviewStatus(q.getReviewStatus());
            rows.add(r);
        }
        String filename = URLEncoder.encode("题库导出-" + courseId + ".xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        EasyExcel.write(response.getOutputStream(), QuestionExcelRow.class).sheet("试题").doWrite(rows);
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        String filename = URLEncoder.encode("试题导入模板.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        EasyExcel.write(response.getOutputStream(), QuestionExcelRow.class).sheet("试题").doWrite(new ArrayList<>());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试题不存在");
        }
        coursePermissionService.assertCourseManageable(q.getCourseId());
        String rs = q.getReviewStatus();
        if (rs == null) {
            rs = QuestionReviewStatusEnum.PUBLISHED.name();
        }
        if (!QuestionReviewStatusEnum.DRAFT.name().equals(rs)
                && !QuestionReviewStatusEnum.REJECTED.name().equals(rs)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅草稿或已驳回的试题可提交审核");
        }
        archiveCurrentAsHistory(q);
        q.setReviewStatus(QuestionReviewStatusEnum.PENDING.name());
        q.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReview(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试题不存在");
        }
        String rs = q.getReviewStatus();
        if (!QuestionReviewStatusEnum.PENDING.name().equals(rs)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅待审核试题可通过");
        }
        archiveCurrentAsHistory(q);
        q.setReviewStatus(QuestionReviewStatusEnum.PUBLISHED.name());
        q.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReview(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试题不存在");
        }
        String rs = q.getReviewStatus();
        if (!QuestionReviewStatusEnum.PENDING.name().equals(rs)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅待审核试题可驳回");
        }
        archiveCurrentAsHistory(q);
        q.setReviewStatus(QuestionReviewStatusEnum.REJECTED.name());
        q.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(q);
    }

    @Override
    public QuestionDedupResultVO dedupCheck(QuestionDedupCheckRequest req) {
        coursePermissionService.assertCourseReadable(req.getCourseId());
        double th = req.getThreshold() != null ? req.getThreshold() : 0.78;
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, req.getCourseId());
        if (req.getKnowledgePointId() != null) {
            w.eq(Question::getKnowledgePointId, req.getKnowledgePointId());
        }
        w.last("LIMIT " + DEDUP_MAX_COMPARE);
        List<Question> pool = questionMapper.selectList(w);
        List<QuestionDedupHitVO> hits = new ArrayList<>();
        for (Question q : pool) {
            if (req.getExcludeQuestionId() != null && req.getExcludeQuestionId().equals(q.getId())) {
                continue;
            }
            double sim = QuestionSimilarityHelper.combinedSimilarity(
                    req.getStem(), req.getOptionsJson(), q.getStem(), q.getOptionsJson());
            if (sim >= th) {
                String stem = q.getStem();
                String preview = stem == null ? "" : (stem.length() > 120 ? stem.substring(0, 120) + "…" : stem);
                hits.add(QuestionDedupHitVO.builder()
                        .questionId(q.getId())
                        .similarity(sim)
                        .type(q.getType())
                        .knowledgePointId(q.getKnowledgePointId())
                        .stemPreview(preview)
                        .build());
            }
        }
        hits.sort(Comparator.comparingDouble(QuestionDedupHitVO::getSimilarity).reversed());
        return QuestionDedupResultVO.builder()
                .hits(hits)
                .comparedTotal(pool.size())
                .truncated(pool.size() >= DEDUP_MAX_COMPARE)
                .build();
    }

    @Override
    public List<QuestionVersionVO> listVersions(Long questionId) {
        Question q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试题不存在");
        }
        coursePermissionService.assertCourseReadable(q.getCourseId());
        List<QuestionVersion> list = questionVersionMapper.selectList(
                new LambdaQueryWrapper<QuestionVersion>()
                        .eq(QuestionVersion::getQuestionId, questionId)
                        .orderByDesc(QuestionVersion::getVersionNo));
        return list.stream().map(this::toVersionVo).collect(Collectors.toList());
    }

    @Override
    public QuestionVersionVO getVersionSnapshot(Long questionId, int versionNo) {
        Question q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试题不存在");
        }
        coursePermissionService.assertCourseReadable(q.getCourseId());
        QuestionVersion v = questionVersionMapper.selectOne(
                new LambdaQueryWrapper<QuestionVersion>()
                        .eq(QuestionVersion::getQuestionId, questionId)
                        .eq(QuestionVersion::getVersionNo, versionNo));
        if (v == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "该历史版本不存在");
        }
        return toVersionVo(v);
    }
}
