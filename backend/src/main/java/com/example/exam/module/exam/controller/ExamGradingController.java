package com.example.exam.module.exam.controller;

import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.exam.dto.SubjectiveGradeRequest;
import com.example.exam.module.exam.dto.SubjectivePendingVO;
import com.example.exam.module.exam.service.ExamGradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exam/grading")
@RequiredArgsConstructor
@Validated
public class ExamGradingController {

    private final ExamGradingService examGradingService;

    @GetMapping("/pending-subjective")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<List<SubjectivePendingVO>> pendingSubjective(@RequestParam Long examId) {
        return ApiResponse.success(examGradingService.listPendingSubjective(examId));
    }

    @PostMapping("/subjective")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Void> subjective(@RequestBody @Valid SubjectiveGradeRequest req) {
        examGradingService.gradeSubjective(req);
        return ApiResponse.success();
    }
}
