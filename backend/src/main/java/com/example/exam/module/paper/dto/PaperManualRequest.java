package com.example.exam.module.paper.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperManualRequest {

    @NotNull
    private Long courseId;

    @NotBlank
    private String title;

    @NotEmpty
    @Valid
    private List<PaperManualItem> items;

    @Data
    public static class PaperManualItem {
        @NotNull
        private Long questionId;
        @NotNull
        private BigDecimal score;
    }
}
