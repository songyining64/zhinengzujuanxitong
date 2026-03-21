package com.example.exam.module.question.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionSaveRequest {

    private Long id;

    @NotNull
    private Long courseId;

    @NotNull
    private Long knowledgePointId;

    private String type;

    private String stem;

    private String optionsJson;

    private String answer;

    private String analysis;

    private BigDecimal scoreDefault;

    private Integer difficulty;

    private Integer status;

    private String reviewStatus;
}
