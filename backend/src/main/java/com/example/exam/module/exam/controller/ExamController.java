package com.example.exam.module.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.exam.dto.ExamCreateRequest;
import com.example.exam.module.exam.dto.ExamStartResponse;
import com.example.exam.module.exam.dto.ExamStudentVO;
import com.example.exam.module.exam.dto.SaveAnswersRequest;
import com.example.exam.module.exam.dto.SubmitAnswerRequest;
import com.example.exam.module.exam.dto.TakeQuestionVO;
import com.example.exam.module.exam.entity.Exam;
import com.example.exam.module.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/exam")
@RequiredArgsConstructor
@Validated
public class ExamController {

    private final ExamService examService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Exam> create(@RequestBody @Valid ExamCreateRequest req) {
        return ApiResponse.success(examService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Exam> update(@PathVariable Long id, @RequestBody @Valid ExamCreateRequest req) {
        return ApiResponse.success(examService.update(id, req));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        examService.publish(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Void> end(@PathVariable Long id) {
        examService.endExam(id);
        return ApiResponse.success();
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Page<Exam>> pageTeacher(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(examService.pageForTeacher(courseId, page, size));
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Page<ExamStudentVO>> pageStudent(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(examService.pageForStudent(page, size, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<Exam> get(@PathVariable Long id) {
        return ApiResponse.success(examService.get(id));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ExamStartResponse> start(@PathVariable Long id) {
        return ApiResponse.success(examService.startExam(id));
    }

    @GetMapping("/record/{recordId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<List<TakeQuestionVO>> questions(@PathVariable Long recordId) {
        return ApiResponse.success(examService.listTakeQuestions(recordId));
    }

    @PostMapping("/record/{recordId}/save-answers")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> save(
            @PathVariable Long recordId,
            @RequestBody @Valid SaveAnswersRequest req
    ) {
        examService.saveAnswers(recordId, req);
        return ApiResponse.success();
    }

    @PostMapping("/record/{recordId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> submit(
            @PathVariable Long recordId,
            @RequestBody @Valid SubmitAnswerRequest req
    ) {
        examService.submitAnswers(recordId, req);
        return ApiResponse.success();
    }
}
