package com.example.exam.module.question.dto;

import lombok.Data;

@Data
public class KnowledgePointUpdateRequest {

    private Long parentId;

    private String name;

    private Integer sortOrder;
}
