package com.example.exam.module.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "课程名称不能为空")
    private String name;

    private String description;

    private String code;

    /** 可选；仅管理员创建时可指定，未指定则授课教师为当前管理员 */
    private Long teacherId;
}
