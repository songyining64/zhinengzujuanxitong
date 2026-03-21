package com.example.exam.module.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.QuestionTypeEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.question.dto.QuestionCreateRequest;
import com.example.exam.module.question.dto.QuestionSaveRequest;
import com.example.exam.module.question.dto.QuestionUpdateRequest;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.mapper.QuestionMapper;
import com.example.exam.module.question.service.QuestionService;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final CoursePermissionService coursePermissionService;
    private final UserService userService;

    private void assertType(String type) {
        if (type == null || !QuestionTypeEnum.isValid(type)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "题型不合法");
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
        if (req.getKnowledgePointId() != null) {
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
                                 long page, long size) {
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
        Page<Question> p = new Page<>(page, size);
        questionMapper.selectPage(p, w.orderByDesc(Question::getUpdateTime));
        return p;
    }
}
