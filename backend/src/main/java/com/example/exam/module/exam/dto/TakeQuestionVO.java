package com.example.exam.module.exam.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TakeQuestionVO {

    private Long questionId;

    private Integer orderNo;

    private String type;

    private String stem;

    private String optionsJson;

    private BigDecimal score;

    private String userAnswer;
}
