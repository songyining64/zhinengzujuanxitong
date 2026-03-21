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

    /**
     * 难度权重（可选）：键 EASY / MEDIUM / HARD，对应题库 difficulty 1/2/3。
     * 若设置，则在每个题型内按权重分配选题数量（如 3:5:2）。
     */
    private Map<String, Integer> difficultyWeights;
}
