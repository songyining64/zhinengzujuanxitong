package com.example.exam.module.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.QuestionTypeEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.exam.dto.SubjectiveGradeRequest;
import com.example.exam.module.exam.entity.Exam;
import com.example.exam.module.exam.entity.ExamAnswer;
import com.example.exam.module.exam.entity.ExamRecord;
import com.example.exam.module.exam.mapper.ExamAnswerMapper;
import com.example.exam.module.exam.mapper.ExamMapper;
import com.example.exam.module.exam.mapper.ExamRecordMapper;
import com.example.exam.module.exam.service.ExamGradingService;
import com.example.exam.module.exam.service.ExamService;
import com.example.exam.module.paper.entity.PaperQuestion;
import com.example.exam.module.paper.mapper.PaperQuestionMapper;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExamGradingServiceImpl implements ExamGradingService {

    private final ExamAnswerMapper examAnswerMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final CoursePermissionService coursePermissionService;
    private final ExamService examService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeSubjective(SubjectiveGradeRequest req) {
        ExamRecord record = examRecordMapper.selectById(req.getExamRecordId());
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试记录不存在");
        }
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试不存在");
        }
        coursePermissionService.assertExamManageable(exam.getId());
        Question q = questionMapper.selectById(req.getQuestionId());
        if (q == null || !QuestionTypeEnum.SHORT.name().equals(q.getType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅支持主观题评分");
        }
        PaperQuestion pq = paperQuestionMapper.selectOne(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, exam.getPaperId())
                .eq(PaperQuestion::getQuestionId, req.getQuestionId()));
        if (pq == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "题目不在试卷中");
        }
        if (req.getScore().compareTo(pq.getScore()) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "得分不能超过题目分值");
        }
        ExamAnswer ans = examAnswerMapper.selectOne(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getExamRecordId, req.getExamRecordId())
                .eq(ExamAnswer::getQuestionId, req.getQuestionId()));
        if (ans == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "作答不存在");
        }
        ans.setScore(req.getScore());
        ans.setGradedAt(LocalDateTime.now());
        ans.setIsCorrect(req.getScore().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
        examAnswerMapper.updateById(ans);
        examService.gradeAndFinalizeRecord(req.getExamRecordId());
    }
}
