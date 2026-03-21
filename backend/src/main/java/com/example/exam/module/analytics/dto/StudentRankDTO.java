package com.example.exam.module.analytics.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentRankDTO {

    private int rank;

    private Long studentId;

    private String username;

    private String realName;

    private BigDecimal totalScore;

    /** 是否达到及格线（未设及格线时为 null） */
    private Boolean passed;
}
