package com.example.exam.module.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDedupHitVO {

    private Long questionId;

    /** 综合相似度 0~1 */
    private double similarity;

    private String type;

    private Long knowledgePointId;

    /** 题干摘要（前 120 字） */
    private String stemPreview;
}
