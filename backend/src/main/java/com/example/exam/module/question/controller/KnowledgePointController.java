package com.example.exam.module.question.controller;

import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.question.dto.KnowledgePointCreateRequest;
import com.example.exam.module.question.dto.KnowledgePointUpdateRequest;
import com.example.exam.module.question.entity.KnowledgePoint;
import com.example.exam.module.question.service.KnowledgePointService;
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
@RequestMapping("/api/knowledge-point")
@RequiredArgsConstructor
@Validated
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgePoint> create(@RequestBody @Valid KnowledgePointCreateRequest req) {
        return ApiResponse.success(knowledgePointService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('knowledge:manage')")
    public ApiResponse<KnowledgePoint> update(@PathVariable Long id, @RequestBody KnowledgePointUpdateRequest req) {
        return ApiResponse.success(knowledgePointService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('knowledge:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgePointService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('knowledge:read')")
    public ApiResponse<List<KnowledgePoint>> list(@RequestParam Long courseId) {
        return ApiResponse.success(knowledgePointService.listByCourse(courseId));
    }
}
