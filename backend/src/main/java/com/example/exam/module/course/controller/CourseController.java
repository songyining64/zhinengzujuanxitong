package com.example.exam.module.course.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.course.dto.CourseCreateRequest;
import com.example.exam.module.course.dto.CourseStudentVO;
import com.example.exam.module.course.dto.CourseUpdateRequest;
import com.example.exam.module.course.entity.Course;
import com.example.exam.module.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
@Validated
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Course> create(@RequestBody @Valid CourseCreateRequest req) {
        return ApiResponse.success(courseService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Course> update(@PathVariable Long id, @RequestBody CourseUpdateRequest req) {
        return ApiResponse.success(courseService.update(id, req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<Course> get(@PathVariable Long id) {
        return ApiResponse.success(courseService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<Page<Course>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(courseService.pageForCurrentUser(page, size, keyword));
    }

    @PostMapping("/{courseId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Void> addStudent(@PathVariable Long courseId, @PathVariable Long studentId) {
        courseService.addStudent(courseId, studentId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{courseId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Void> removeStudent(@PathVariable Long courseId, @PathVariable Long studentId) {
        courseService.removeStudent(courseId, studentId);
        return ApiResponse.success();
    }

    @GetMapping("/{courseId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<List<CourseStudentVO>> students(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.listStudents(courseId));
    }
}
