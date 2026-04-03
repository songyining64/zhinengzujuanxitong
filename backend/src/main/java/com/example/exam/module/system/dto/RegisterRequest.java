package com.example.exam.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 公开注册（仅创建学生账号），与 WebStudy 类项目对齐的简化表单 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度为 3～64")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码至少 6 位")
    private String password;

    @NotBlank(message = "请确认密码")
    private String confirmPassword;

    @Size(max = 64, message = "姓名过长")
    private String realName;
}
