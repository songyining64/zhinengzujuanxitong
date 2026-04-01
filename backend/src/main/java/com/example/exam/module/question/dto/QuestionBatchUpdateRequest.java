package com.example.exam.module.question.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionBatchUpdateRequest {

    @NotNull
    private Long courseId;

    @NotEmpty
    private List<Long> questionIds;

    private Long knowledgePointId;

    private Integer difficulty;
}
