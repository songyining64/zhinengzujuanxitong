package com.example.exam.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('system:user:manage')")
    public ApiResponse<Page<User>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size
    ) {
        Page<User> result = userService.pageUsers(keyword, page, size);
        return ApiResponse.success(result);
    }

    @Data
    public static class UserVO {
        private Long id;
        private String username;
        private String realName;
        private String role;
        private Integer status;
    }
}

