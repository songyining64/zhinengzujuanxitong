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
}
