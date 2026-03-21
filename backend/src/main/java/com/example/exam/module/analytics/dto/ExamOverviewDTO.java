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

    /** 及格线（可能为 null） */
    private BigDecimal passScore;

    /** 成绩是否已发布 */
    private Boolean scorePublished;

    /** 达及格人数（仅教师端统计有意义） */
    private Long passCount;
}
