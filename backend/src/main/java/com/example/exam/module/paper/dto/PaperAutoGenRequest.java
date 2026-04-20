package com.example.exam.module.paper.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PaperAutoGenRequest {

    @NotNull
    private Long courseId;

    /** 生成试卷时必填；保存为组卷模板时可空（由服务层校验） */
    private String title;

    /**
     * 知识点 ID 列表；当 {@link #randomPool} 为 true 时忽略。
     */
    private List<Long> knowledgePointIds;

    /** 为 true 时从全课程已发布试题中随机抽题（仍按题型与难度配比） */
    private Boolean randomPool = Boolean.FALSE;

    /** 必考题目 ID，按顺序排在卷首；须为本课程已发布试题 */
    private List<Long> fixedQuestionIds;

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

    /**
     * 按题型配置分值（如 SINGLE -> 2、SHORT -> 10）；未配置的题型使用 {@link #scorePerQuestion} 或默认 10。
     */
    private Map<String, BigDecimal> scoreByType;

    /** 期望试卷总分；若与按题型分值推算不一致且未允许放宽，将失败并提示 */
    private BigDecimal targetTotalScore;

    /**
     * 知识点覆盖率下限（0–1），如 0.95。仅在与 {@link #randomPool} 为 false 且指定了知识点时生效。
     */
    private BigDecimal minKnowledgeCoverage;

    /**
     * 未达覆盖率等约束时是否仍生成试卷并标记 {@link PaperAutoGenResult#isPartialConstraint()}。
     */
    private Boolean allowPartialConstraints = Boolean.FALSE;

    /** 显式排除的题目 ID（已做卷、人工指定等） */
    private List<Long> excludeQuestionIds;

    /** 禁选题：永不入卷（与 exclude 合并过滤） */
    private List<Long> forbiddenQuestionIds;

    /**
     * 排除某学生错题本中的试题（教师巩固卷 / 个性化组卷）。
     */
    private Long excludeWrongBookForStudentId;

    /**
     * GREEDY / GENETIC（遗传为预留，当前实现为贪心 + 修复）。
     */
    private String algorithmMode;
}
