package com.example.exam.module.analytics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.analytics.dto.ExamKnowledgeStatDTO;
import com.example.exam.module.analytics.dto.ExamOverviewDTO;
import com.example.exam.module.analytics.dto.ExamQuestionStatDTO;
import com.example.exam.module.analytics.dto.StudentRankDTO;
import com.example.exam.module.analytics.service.ExamAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/exam/analytics")
@RequiredArgsConstructor
public class ExamAnalyticsController {

    private final ExamAnalyticsService examAnalyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<ExamOverviewDTO> overview(@RequestParam Long examId) {
        return ApiResponse.success(examAnalyticsService.overview(examId));
    }

    @GetMapping("/rank")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<Page<StudentRankDTO>> rank(
            @RequestParam Long examId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(examAnalyticsService.rank(examId, page, size));
    }

    @GetMapping("/question-stats")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<List<ExamQuestionStatDTO>> questionStats(@RequestParam Long examId) {
        return ApiResponse.success(examAnalyticsService.questionStats(examId));
    }

    @GetMapping("/knowledge-point-stats")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<List<ExamKnowledgeStatDTO>> kpStats(@RequestParam Long examId) {
        return ApiResponse.success(examAnalyticsService.knowledgePointStats(examId));
    }

    @GetMapping("/export-rank")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public void exportRank(@RequestParam Long examId, HttpServletResponse response) throws IOException {
        examAnalyticsService.exportRank(examId, response);
    }
}
