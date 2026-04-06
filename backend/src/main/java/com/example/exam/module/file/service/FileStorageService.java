package com.example.exam.module.file.service;

import com.example.exam.module.file.entity.FileResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileResource store(MultipartFile file);

    FileResource getMeta(Long id);

    Resource loadAsResource(Long id);
}
