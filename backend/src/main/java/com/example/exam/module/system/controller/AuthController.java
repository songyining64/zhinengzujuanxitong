package com.example.exam.module.system.controller;

import com.example.exam.common.api.ApiResponse;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.SysMenuService;
import com.example.exam.module.system.service.UserService;
import com.example.exam.security.JwtTokenUtil;
import com.example.exam.module.system.dto.RegisterRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;
    private final SysMenuService sysMenuService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenUtil.generateToken(request.getUsername());
        User user = userService.findByUsername(request.getUsername());

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole());
        resp.setPermissions(sysMenuService.listGrantedPermsForRole(user.getRole()));
        return ApiResponse.success(resp);
    }

    /** 自助注册为学生账号（无需登录），见 {@link UserService#registerStudent} */
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody @Valid RegisterRequest request) {
        userService.registerStudent(request);
        return ApiResponse.success();
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private String username;
        private String realName;
        private String role;
        /** 与后端接口权限一致的细粒度权限码，供前端控制按钮显隐 */
        private List<String> permissions;
    }
}

