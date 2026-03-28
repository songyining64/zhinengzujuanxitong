package com.example.exam.module.exam.controller;

import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.exam.dto.SubjectiveGradeRequest;
import com.example.exam.module.exam.service.ExamGradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exam/grading")
@RequiredArgsConstructor
@Validated
public class ExamGradingController {

    private final ExamGradingService examGradingService;

    @PostMapping("/subjective")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Void> subjective(@RequestBody @Valid SubjectiveGradeRequest req) {
        examGradingService.gradeSubjective(req);
        return ApiResponse.success();
    }
}
