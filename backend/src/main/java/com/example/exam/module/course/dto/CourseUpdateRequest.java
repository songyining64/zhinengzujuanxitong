package com.example.exam.module.course.dto;

import lombok.Data;

@Data
public class CourseUpdateRequest {

    private String name;

    private String description;

    private String code;

    private Integer status;
}
