package com.example.exam.module.file.controller;

import com.example.exam.common.api.ApiResponse;
import com.example.exam.config.AppProperties;
import com.example.exam.module.file.dto.FileUploadVO;
import com.example.exam.module.file.entity.FileResource;
import com.example.exam.module.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final AppProperties appProperties;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<FileUploadVO> upload(MultipartFile file) {
        FileResource fr = fileStorageService.store(file);
        FileUploadVO vo = new FileUploadVO();
        vo.setId(fr.getId());
        vo.setOriginalName(fr.getOriginalName());
        vo.setSizeBytes(fr.getSizeBytes());
        vo.setDownloadUrl(appProperties.getDownloadPathPrefix() + "/" + fr.getId());
        return ApiResponse.success(vo);
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileResource meta = fileStorageService.getMeta(id);
        Resource body = fileStorageService.loadAsResource(id);
        String encoded = URLEncoder.encode(meta.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        if (meta.getContentType() != null && !meta.getContentType().isBlank()) {
            try {
                mt = MediaType.parseMediaType(meta.getContentType());
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(mt)
                .body(body);
    }
}
