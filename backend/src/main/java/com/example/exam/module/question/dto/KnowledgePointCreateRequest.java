package com.example.exam.module.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgePointCreateRequest {

    @NotNull(message = "课程不能为空")
    private Long courseId;

    private Long parentId;

    @NotBlank(message = "名称不能为空")
    private String name;

    private Integer sortOrder;
}
