package com.example.exam.module.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubjectiveGradeRequest {

    @NotNull
    private Long examRecordId;

    @NotNull
    private Long questionId;

    @NotNull
    private BigDecimal score;
}
