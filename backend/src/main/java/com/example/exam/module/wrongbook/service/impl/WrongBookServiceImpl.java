package com.example.exam.module.wrongbook.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.exam.entity.Exam;
import com.example.exam.module.exam.entity.ExamAnswer;
import com.example.exam.module.exam.entity.ExamRecord;
import com.example.exam.module.exam.mapper.ExamAnswerMapper;
import com.example.exam.module.exam.mapper.ExamMapper;
import com.example.exam.module.exam.mapper.ExamRecordMapper;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.mapper.QuestionMapper;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import com.example.exam.module.wrongbook.dto.WrongBookVO;
import com.example.exam.module.wrongbook.entity.WrongBook;
import com.example.exam.module.wrongbook.mapper.WrongBookMapper;
import com.example.exam.module.wrongbook.service.WrongBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WrongBookServiceImpl implements WrongBookService {

    private final WrongBookMapper wrongBookMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final UserService userService;
    private final CoursePermissionService coursePermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAfterExamSubmit(Long examRecordId) {
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        if (record == null) {
            return;
        }
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam == null) {
            return;
        }
        Long courseId = exam.getCourseId();
        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getExamRecordId, examRecordId));
        for (ExamAnswer a : answers) {
            if (a.getIsCorrect() != null && a.getIsCorrect() == 0) {
                upsertWrong(record.getStudentId(), courseId, a.getQuestionId());
            }
        }
    }

    private void upsertWrong(Long studentId, Long courseId, Long questionId) {
        WrongBook wb = wrongBookMapper.selectOne(new LambdaQueryWrapper<WrongBook>()
                .eq(WrongBook::getStudentId, studentId)
                .eq(WrongBook::getQuestionId, questionId));
        LocalDateTime now = LocalDateTime.now();
        if (wb == null) {
            wb = new WrongBook();
            wb.setStudentId(studentId);
            wb.setCourseId(courseId);
            wb.setQuestionId(questionId);
            wb.setWrongCount(1);
            wb.setLastWrongAt(now);
            wb.setCreateTime(now);
            wb.setUpdateTime(now);
            wrongBookMapper.insert(wb);
        } else {
            wb.setWrongCount(wb.getWrongCount() == null ? 1 : wb.getWrongCount() + 1);
            wb.setLastWrongAt(now);
            wb.setUpdateTime(now);
            wrongBookMapper.updateById(wb);
        }
    }

    @Override
    public Page<WrongBookVO> pageForStudent(Long courseId, long page, long size) {
        coursePermissionService.assertCourseReadable(courseId);
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null || !RoleEnum.STUDENT.name().equals(u.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅学生可查看错题本");
        }
        LambdaQueryWrapper<WrongBook> w = new LambdaQueryWrapper<WrongBook>()
                .eq(WrongBook::getStudentId, u.getId())
                .eq(WrongBook::getCourseId, courseId);
        Page<WrongBook> p = new Page<>(page, size);
        wrongBookMapper.selectPage(p, w.orderByDesc(WrongBook::getLastWrongAt));
        List<WrongBookVO> list = new ArrayList<>();
        for (WrongBook wb : p.getRecords()) {
            WrongBookVO v = new WrongBookVO();
            v.setId(wb.getId());
            v.setCourseId(wb.getCourseId());
            v.setQuestionId(wb.getQuestionId());
            v.setWrongCount(wb.getWrongCount());
            v.setLastWrongAt(wb.getLastWrongAt());
            Question q = questionMapper.selectById(wb.getQuestionId());
            if (q != null) {
                v.setStem(q.getStem());
                v.setType(q.getType());
            }
            list.add(v);
        }
        Page<WrongBookVO> vo = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        vo.setRecords(list);
        return vo;
    }
}
