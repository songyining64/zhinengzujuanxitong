package com.example.exam.module.question.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionUpdateRequest {

    private Long knowledgePointId;

    private String type;

    private String stem;

    private String optionsJson;

    private String answer;

    private String analysis;

    private BigDecimal scoreDefault;

    private Integer difficulty;

    private Integer status;
}
