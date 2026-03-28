package com.example.exam.module.paper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.paper.dto.PaperAutoGenRequest;
import com.example.exam.module.paper.dto.PaperDetailVO;
import com.example.exam.module.paper.dto.PaperFromTemplateRequest;
import com.example.exam.module.paper.dto.PaperManualRequest;
import com.example.exam.module.paper.entity.Paper;
import com.example.exam.module.paper.entity.PaperGenerationLog;
import com.example.exam.module.paper.service.PaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paper")
@RequiredArgsConstructor
@Validated
public class PaperController {

    private final PaperService paperService;

    @PostMapping("/manual")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<Paper> manual(@RequestBody @Valid PaperManualRequest req) {
        return ApiResponse.success(paperService.createManual(req));
    }

    @PostMapping("/auto-generate")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<Paper> auto(@RequestBody @Valid PaperAutoGenRequest req) {
        return ApiResponse.success(paperService.generateAuto(req));
    }

    @PostMapping("/from-template/{templateId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<Paper> fromTemplate(
            @PathVariable Long templateId,
            @RequestBody @Valid PaperFromTemplateRequest req
    ) {
        return ApiResponse.success(paperService.generateFromTemplate(templateId, req));
    }

    @GetMapping("/generation-logs")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:read')")
    public ApiResponse<Page<PaperGenerationLog>> generationLogs(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(paperService.pageGenerationLogs(courseId, page, size));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:read')")
    public ApiResponse<Page<Paper>> page(
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(paperService.page(courseId, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:read')")
    public ApiResponse<PaperDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(paperService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        paperService.delete(id);
        return ApiResponse.success();
    }
}
