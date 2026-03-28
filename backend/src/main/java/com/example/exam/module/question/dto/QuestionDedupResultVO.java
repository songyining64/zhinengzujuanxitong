package com.example.exam.module.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDedupResultVO {

    @Builder.Default
    private List<QuestionDedupHitVO> hits = new ArrayList<>();

    /** 参与比对的题目数量 */
    private int comparedTotal;

    /** 是否因上限截断（仅比对前 N 条） */
    private boolean truncated;
}
