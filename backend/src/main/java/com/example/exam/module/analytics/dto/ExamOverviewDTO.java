package com.example.exam.module.analytics.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExamOverviewDTO {

    private Long examId;

    private long submittedCount;

    private long totalRecords;

    private BigDecimal avgScore;

    private BigDecimal maxScore;

    private BigDecimal minScore;
}
