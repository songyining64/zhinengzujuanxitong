package com.example.exam.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "exam")
public class ExamProperties {

    private long autoSubmitIntervalMs = 60_000L;

    /** 组卷算法与超时（多约束场景） */
    private PaperCompose paperCompose = new PaperCompose();

    @Getter
    @Setter
    public static class PaperCompose {
        /** 超过该毫秒数仅在结果中提示，不中断已完成的组卷 */
        private long composeTimeoutMs = 10_000L;
        /** 贪心修复：尝试用同题型替换以提升知识点覆盖的次数上限 */
        private int greedyMaxRepairSwaps = 800;
        /** 预留：遗传算法交叉概率（0–1），当前未启用 */
        private double geneticCrossoverRate = 0.7;
        /** 预留：遗传算法变异概率（0–1），当前未启用 */
        private double geneticMutationRate = 0.05;
        /** 默认算法：GREEDY（遗传/退火为后续扩展） */
        private String defaultAlgorithm = "GREEDY";
    }
}
