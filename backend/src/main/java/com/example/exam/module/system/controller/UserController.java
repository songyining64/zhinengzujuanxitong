package com.example.exam.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('system:user:manage')")
    public ApiResponse<UserVO> create(@RequestBody @Validated UserCreateRequest req) {
        User u = userService.createUser(
                req.getUsername(),
                req.getPassword(),
                req.getRealName(),
                req.getRole(),
                req.getStatus()
        );
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setRole(u.getRole());
        vo.setStatus(u.getStatus());
        return ApiResponse.success(vo);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('system:user:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    @Data
    public static class UserCreateRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 64, message = "用户名长度为 2～64")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度为 6～64")
        private String password;

        @Size(max = 64, message = "姓名过长")
        private String realName;

        @NotBlank(message = "角色不能为空")
        private String role;

        @NotNull(message = "状态不能为空")
        private Integer status;
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

