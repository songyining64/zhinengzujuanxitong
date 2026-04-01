package com.example.exam.module.course.service;

public interface CoursePermissionService {

    void assertCourseReadable(Long courseId);

    void assertCourseManageable(Long courseId);

    void assertExamReadable(Long examId);

    void assertExamManageable(Long examId);
}
