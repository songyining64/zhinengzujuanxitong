package com.example.exam.module.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseVO {

    private Long id;

    private Long teacherId;

    /**
     * 展示用的授课教师名称（realName 优先，否则 fallback 到 username）
     */
    private String teacherName;

    private String name;

    private String description;

    private String code;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

