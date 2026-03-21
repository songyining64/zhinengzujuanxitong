package com.example.exam.module.paper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PaperAutoGenRequest {

    @NotNull
    private Long courseId;

    @NotBlank
    private String title;

    @NotNull
    private List<Long> knowledgePointIds;

    /** 是否包含知识点子孙节点 */
    private Boolean includeKnowledgeDescendants = Boolean.TRUE;

    private BigDecimal scorePerQuestion;

    private Long randomSeed;

    /** 选题时是否题目去重 */
    private Boolean dedup = Boolean.TRUE;

    /** 题型 -> 数量，如 SINGLE -> 5 */
    private Map<String, Integer> countByType;
}
