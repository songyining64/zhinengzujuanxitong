package com.example.exam.module.question.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.question.dto.QuestionBatchUpdateRequest;
import com.example.exam.module.question.dto.QuestionCreateRequest;
import com.example.exam.module.question.dto.QuestionDedupCheckRequest;
import com.example.exam.module.question.dto.QuestionDedupResultVO;
import com.example.exam.module.question.dto.QuestionImportResultVO;
import com.example.exam.module.question.dto.QuestionSaveRequest;
import com.example.exam.module.question.dto.QuestionUpdateRequest;
import com.example.exam.module.question.dto.QuestionVersionVO;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.service.QuestionService;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:read')")
    public ApiResponse<Page<Question>> page(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(questionService.page(courseId, knowledgePointId, type, keyword, reviewStatus, page, size));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:manage')")
    public ApiResponse<Integer> batchUpdate(@RequestBody @Valid QuestionBatchUpdateRequest req) {
        return ApiResponse.success(questionService.batchUpdate(req));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:manage')")
    public ApiResponse<QuestionImportResultVO> importExcel(
            @RequestParam Long courseId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(questionService.importExcel(courseId, file));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:read')")
    public void exportExcel(@RequestParam Long courseId, HttpServletResponse response) throws IOException {
        questionService.exportExcel(courseId, response);
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:manage')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        questionService.downloadImportTemplate(response);
    }

    @PostMapping("/dedup-check")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:read')")
    public ApiResponse<QuestionDedupResultVO> dedupCheck(@RequestBody @Valid QuestionDedupCheckRequest req) {
        return ApiResponse.success(questionService.dedupCheck(req));
    }

    @GetMapping("/{id}/versions/{versionNo}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:read')")
    public ApiResponse<QuestionVersionVO> getVersion(
            @PathVariable Long id,
            @PathVariable int versionNo
    ) {
        return ApiResponse.success(questionService.getVersionSnapshot(id, versionNo));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:read')")
    public ApiResponse<List<QuestionVersionVO>> listVersions(@PathVariable Long id) {
        return ApiResponse.success(questionService.listVersions(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:read')")
    public ApiResponse<Question> get(@PathVariable Long id) {
        return ApiResponse.success(questionService.get(id));
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('question:manage')")
    public ApiResponse<Void> submitReview(@PathVariable Long id) {
        questionService.submitReview(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        questionService.approveReview(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        questionService.rejectReview(id);
        return ApiResponse.success();
    }
}
