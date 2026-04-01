package com.example.exam.module.file.dto;

import lombok.Data;

@Data
public class FileUploadVO {

    private Long id;

    private String originalName;

    private String downloadUrl;

    private Long sizeBytes;
}
