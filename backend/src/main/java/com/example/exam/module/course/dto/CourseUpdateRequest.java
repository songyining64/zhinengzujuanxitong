package com.example.exam.module.course.dto;

import lombok.Data;

@Data
public class CourseUpdateRequest {

    private String name;

    private String description;

    private String code;

    private Integer status;

    /** 可选；仅管理员可修改 */
    private Long teacherId;
}
