package com.example.exam.module.exam.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamStudentVO {

    private Long id;

    private Long courseId;

    private String courseName;

    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer durationMinutes;

    private String status;
}
