package com.example.exam.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private String realName;

    @NotBlank
    private String role;

    /** 0 禁用，1 启用；默认 1 */
    private Integer status;
}
