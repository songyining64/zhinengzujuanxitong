package com.example.exam.module.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SaveAnswersRequest {

    /** 允许空列表（仅心跳保存时无变更） */
    @NotNull
    @Valid
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String userAnswer;
    }
}
