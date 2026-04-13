package com.example.exam.module.system.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String realName;

    /** ADMIN / TEACHER / STUDENT，null 表示不改 */
    private String role;

    /** 0 禁用，1 启用 */
    private Integer status;

    /** 留空则不修改密码 */
    private String password;
}
