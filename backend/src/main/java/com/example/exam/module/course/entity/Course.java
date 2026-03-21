package com.example.exam.module.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private String name;

    private String description;

    private String code;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
