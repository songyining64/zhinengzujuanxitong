package com.example.exam.module.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuestionCreateRequest {

    @NotNull
    private Long courseId;

    @NotNull
    private Long knowledgePointId;

    @NotBlank
    private String type;

    @NotBlank
    private String stem;

    private String optionsJson;

    @NotBlank
    private String answer;

    private String analysis;

    private BigDecimal scoreDefault;

    private Integer difficulty;

    /** 可选：DRAFT/PENDING/PUBLISHED/REJECTED，默认 PUBLISHED */
    private String reviewStatus;
}
