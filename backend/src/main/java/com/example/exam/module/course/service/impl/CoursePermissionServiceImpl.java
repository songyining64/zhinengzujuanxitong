package com.example.exam.module.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.course.entity.Course;
import com.example.exam.module.course.entity.CourseStudent;
import com.example.exam.module.course.mapper.CourseMapper;
import com.example.exam.module.course.mapper.CourseStudentMapper;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.exam.entity.Exam;
import com.example.exam.module.exam.mapper.ExamMapper;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoursePermissionServiceImpl implements CoursePermissionService {

    private final UserService userService;
    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ExamMapper examMapper;

    private User currentUser() {
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return u;
    }

    @Override
    public void assertCourseReadable(Long courseId) {
        Course course = requireCourse(courseId);
        User u = currentUser();
        if (RoleEnum.ADMIN.name().equals(u.getRole())) {
            return;
        }
        if (RoleEnum.TEACHER.name().equals(u.getRole())) {
            if (course.getTeacherId() != null && course.getTeacherId().equals(u.getId())) {
                return;
            }
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该课程");
        }
        if (RoleEnum.STUDENT.name().equals(u.getRole())) {
            Long n = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getStudentId, u.getId())
                    .eq(CourseStudent::getStatus, 1));
            if (n != null && n > 0) {
                return;
            }
            throw new BizException(ErrorCode.FORBIDDEN, "未加入该课程");
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权查看该课程");
    }

    @Override
    public void assertCourseManageable(Long courseId) {
        Course course = requireCourse(courseId);
        User u = currentUser();
        if (RoleEnum.ADMIN.name().equals(u.getRole())) {
            return;
        }
        if (RoleEnum.TEACHER.name().equals(u.getRole())) {
            if (course.getTeacherId() != null && course.getTeacherId().equals(u.getId())) {
                return;
            }
        }
        throw new BizException(ErrorCode.FORBIDDEN, "无权管理该课程");
    }

    @Override
    public void assertExamReadable(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试不存在");
        }
        assertCourseReadable(exam.getCourseId());
    }

    @Override
    public void assertExamManageable(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "考试不存在");
        }
        assertCourseManageable(exam.getCourseId());
    }

    private Course requireCourse(Long courseId) {
        Course c = courseMapper.selectById(courseId);
        if (c == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return c;
    }
}
