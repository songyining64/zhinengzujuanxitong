package com.example.exam.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    /**
     * 角色：ADMIN / TEACHER / STUDENT
     */
    private String role;

    /**
     * 状态：0 禁用，1 启用
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

