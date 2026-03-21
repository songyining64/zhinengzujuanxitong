package com.example.exam.module.course.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.course.dto.CourseCreateRequest;
import com.example.exam.module.course.dto.CourseStudentVO;
import com.example.exam.module.course.dto.CourseUpdateRequest;
import com.example.exam.module.course.entity.Course;

import java.util.List;

public interface CourseService {

    Course create(CourseCreateRequest req);

    Course update(Long id, CourseUpdateRequest req);

    Course get(Long id);

    Page<Course> pageForCurrentUser(long page, long size, String keyword);

    void addStudent(Long courseId, Long studentId);

    void removeStudent(Long courseId, Long studentId);

    List<CourseStudentVO> listStudents(Long courseId);
}
