package com.example.exam.module.paper.controller;

import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.paper.dto.PaperTemplateSaveRequest;
import com.example.exam.module.paper.entity.PaperTemplate;
import com.example.exam.module.paper.service.PaperTemplateService;
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
@RequestMapping("/api/paper-template")
@RequiredArgsConstructor
@Validated
public class PaperTemplateController {

    private final PaperTemplateService paperTemplateService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<PaperTemplate> create(@RequestBody @Valid PaperTemplateSaveRequest req) {
        return ApiResponse.success(paperTemplateService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<PaperTemplate> update(@PathVariable Long id, @RequestBody @Valid PaperTemplateSaveRequest req) {
        return ApiResponse.success(paperTemplateService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        paperTemplateService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:read')")
    public ApiResponse<List<PaperTemplate>> list(@RequestParam Long courseId) {
        return ApiResponse.success(paperTemplateService.listByCourse(courseId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('paper:read')")
    public ApiResponse<PaperTemplate> get(@PathVariable Long id) {
        return ApiResponse.success(paperTemplateService.get(id));
    }
}
