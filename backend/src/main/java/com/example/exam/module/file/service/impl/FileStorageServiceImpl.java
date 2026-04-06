package com.example.exam.module.file.service.impl;

import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.config.AppProperties;
import com.example.exam.module.file.entity.FileResource;
import com.example.exam.module.file.mapper.FileResourceMapper;
import com.example.exam.module.file.service.FileStorageService;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FileResourceMapper fileResourceMapper;
    private final AppProperties appProperties;
    private final UserService userService;

    private Path baseDir() {
        return Paths.get(appProperties.getDir()).toAbsolutePath().normalize();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResource store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件为空");
        }
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        try {
            Files.createDirectories(baseDir());
            String ext = "";
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) {
                ext = original.substring(dot);
            }
            String stored = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = baseDir().resolve(stored);
            file.transferTo(target.toFile());
            FileResource fr = new FileResource();
            fr.setOriginalName(original);
            fr.setStoredName(stored);
            fr.setRelativePath(stored);
            fr.setContentType(file.getContentType());
            fr.setSizeBytes(file.getSize());
            fr.setUploaderId(u.getId());
            fr.setCreateTime(LocalDateTime.now());
            fileResourceMapper.insert(fr);
            return fr;
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件保存失败", e);
        }
    }

    @Override
    public FileResource getMeta(Long id) {
        FileResource fr = fileResourceMapper.selectById(id);
        if (fr == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return fr;
    }

    @Override
    public Resource loadAsResource(Long id) {
        FileResource fr = getMeta(id);
        Path p = baseDir().resolve(fr.getRelativePath()).normalize();
        if (!p.startsWith(baseDir())) {
            throw new BizException(ErrorCode.FORBIDDEN, "非法路径");
        }
        if (!Files.exists(p)) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件已丢失");
        }
        return new FileSystemResource(p.toFile());
    }
}
