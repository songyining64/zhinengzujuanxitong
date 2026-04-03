package com.example.exam.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "ADMIN|TEACHER|STUDENT", message = "角色须为 ADMIN、TEACHER 或 STUDENT")
    private String role;

    private Integer status = 1;
}
