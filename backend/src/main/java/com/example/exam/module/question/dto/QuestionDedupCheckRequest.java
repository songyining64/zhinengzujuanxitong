package com.example.exam.module.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionDedupCheckRequest {

    @NotNull
    private Long courseId;

    @NotBlank
    private String stem;

    /** 与题库中 options_json 比对 */
    private String optionsJson;

    /** 编辑某题时排除自身 */
    private Long excludeQuestionId;

    /** 仅与同一知识点下试题比对，可减轻计算量 */
    private Long knowledgePointId;

    /** 相似度阈值，默认 0.78 */
    private Double threshold;
}
