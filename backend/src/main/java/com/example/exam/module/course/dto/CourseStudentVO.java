package com.example.exam.module.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseStudentVO {

    private Long id;

    private Long studentId;

    private String username;

    private String realName;

    private Integer status;

    private LocalDateTime joinTime;
}
