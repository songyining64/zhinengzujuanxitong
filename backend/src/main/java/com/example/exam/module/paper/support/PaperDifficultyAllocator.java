package com.example.exam.module.paper.support;

/**
 * 自动组卷：按 EASY/MEDIUM/HARD 权重将某题型需求量拆成三档题量（纯函数，便于单测）。
 */
public final class PaperDifficultyAllocator {

    private PaperDifficultyAllocator() {
    }

    /**
     * @param need 该题型总需求
     * @param w1   EASY 权重
     * @param w2   MEDIUM 权重
     * @param w3   HARD 权重
     * @return [易档题量, 中档题量, 难档题量]，三者之和等于 need（权重和为 0 时退化为 [need,0,0]）
     */
    public static int[] allocateNeedByWeights(int need, int w1, int w2, int w3) {
        int sum = w1 + w2 + w3;
        if (sum <= 0) {
            return new int[]{need, 0, 0};
        }
        int e = (int) Math.floor((double) need * w1 / sum);
        int m = (int) Math.floor((double) need * w2 / sum);
        int h = need - e - m;
        return new int[]{e, m, h};
    }
}
