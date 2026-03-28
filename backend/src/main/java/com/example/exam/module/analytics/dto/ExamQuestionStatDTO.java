package com.example.exam.module.analytics.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExamQuestionStatDTO {

    private Long questionId;

    private String type;

    private long attemptCount;

    private long correctCount;

    private BigDecimal correctRate;
}
