package com.example.exam.module.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmitAnswerRequest {

    @NotEmpty
    @Valid
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String userAnswer;
    }
}
