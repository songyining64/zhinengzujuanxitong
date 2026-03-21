package com.example.exam.module.exam.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamStartResponse {

    private Long recordId;

    private Long examId;

    private LocalDateTime startedAt;

    private Integer durationMinutes;

    private LocalDateTime deadlineAt;
}
