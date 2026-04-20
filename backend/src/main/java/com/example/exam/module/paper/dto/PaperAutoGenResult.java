package com.example.exam.module.paper.dto;

import com.example.exam.module.paper.entity.Paper;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能组卷结果：试卷 + 约束达成情况与提示（组卷网式多约束场景）。
 */
@Data
public class PaperAutoGenResult {

    private Paper paper;

    /** 知识点覆盖率（0–1），全库随机或无知识点范围时为 1 */
    private BigDecimal knowledgeCoverage = BigDecimal.ONE;

    /** 是否部分约束未完全满足（仍可生成可用试卷） */
    private boolean partialConstraint;

    private List<String> warnings = new ArrayList<>();

    private List<String> suggestions = new ArrayList<>();

    private long durationMs;

    private String algorithmMode = "GREEDY";
}
