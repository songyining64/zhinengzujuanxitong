package com.example.exam.module.question.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.question.dto.QuestionCreateRequest;
import com.example.exam.module.question.dto.QuestionSaveRequest;
import com.example.exam.module.question.dto.QuestionUpdateRequest;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.service.QuestionService;
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

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
@Validated
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:manage')")
    public ApiResponse<Question> create(@RequestBody @Valid QuestionCreateRequest req) {
        return ApiResponse.success(questionService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:manage')")
    public ApiResponse<Question> update(@PathVariable Long id, @RequestBody QuestionUpdateRequest req) {
        return ApiResponse.success(questionService.update(id, req));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:manage')")
    public ApiResponse<Question> save(@RequestBody @Valid QuestionSaveRequest req) {
        return ApiResponse.success(questionService.save(req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:read')")
    public ApiResponse<Question> get(@PathVariable Long id) {
        return ApiResponse.success(questionService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:read')")
    public ApiResponse<Page<Question>> page(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(questionService.page(courseId, knowledgePointId, type, keyword, page, size));
    }
}
