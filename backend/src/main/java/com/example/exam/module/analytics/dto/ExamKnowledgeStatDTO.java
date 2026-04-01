package com.example.exam.module.analytics.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExamKnowledgeStatDTO {

    private Long knowledgePointId;

    private String knowledgePointName;

    private long questionCount;

    private BigDecimal avgCorrectRate;
}
