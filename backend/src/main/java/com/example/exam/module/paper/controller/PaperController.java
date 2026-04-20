package com.example.exam.module.paper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ApiResponse;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.exception.BizException;
import com.example.exam.module.paper.dto.PaperAutoGenRequest;
import com.example.exam.module.paper.dto.PaperAutoGenResult;
import com.example.exam.module.paper.dto.PaperDetailVO;
import com.example.exam.module.paper.dto.PaperFromTemplateRequest;
import com.example.exam.module.paper.dto.PaperManualRequest;
import com.example.exam.module.paper.entity.Paper;
import com.example.exam.module.paper.entity.PaperGenerationLog;
import com.example.exam.module.paper.service.PaperComposeTaskService;
import com.example.exam.module.paper.service.PaperService;
import com.example.exam.module.paper.support.PaperComposeTaskRegistry;
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
    private final PaperComposeTaskService paperComposeTaskService;

    @PostMapping("/manual")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<Paper> manual(@RequestBody @Valid PaperManualRequest req) {
        return ApiResponse.success(paperService.createManual(req));
    }

    @PostMapping("/auto-generate")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<PaperAutoGenResult> auto(@RequestBody @Valid PaperAutoGenRequest req) {
        return ApiResponse.success(paperService.generateAuto(req));
    }

    /**
     * 异步组卷：立即返回 taskId，前端轮询 {@link #composeTask(String)}。
     */
    @PostMapping("/auto-generate-async")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<String> autoAsync(@RequestBody @Valid PaperAutoGenRequest req) {
        return ApiResponse.success(paperComposeTaskService.submit(req));
    }

    @GetMapping("/compose-tasks/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<PaperComposeTaskRegistry.TaskEntry> composeTask(@PathVariable String taskId) {
        PaperComposeTaskRegistry.TaskEntry e = paperComposeTaskService.get(taskId);
        if (e == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "组卷任务不存在");
        }
        return ApiResponse.success(e);
    }

    @PostMapping("/from-template/{templateId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<PaperAutoGenResult> fromTemplate(
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
