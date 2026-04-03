package com.example.exam.module.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "课程名称不能为空")
    private String name;

    private String description;

    private String code;

    /** 管理员创建课程时必填：指定授课教师用户 ID */
    private Long teacherId;
}
