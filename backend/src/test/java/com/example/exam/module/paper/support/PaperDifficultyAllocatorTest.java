package com.example.exam.module.paper.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperDifficultyAllocatorTest {

    @Test
    void allocate_3_5_2_sumMatchesNeed() {
        int[] a = PaperDifficultyAllocator.allocateNeedByWeights(10, 3, 5, 2);
        assertEquals(10, a[0] + a[1] + a[2]);
        assertArrayEquals(new int[]{3, 5, 2}, a);
    }

    @Test
    void allocate_weightsZero_fallsBackToAllEasy() {
        int[] a = PaperDifficultyAllocator.allocateNeedByWeights(7, 0, 0, 0);
        assertArrayEquals(new int[]{7, 0, 0}, a);
    }

    @Test
    void allocate_remainderGoesToLastTier() {
        int[] a = PaperDifficultyAllocator.allocateNeedByWeights(10, 1, 1, 1);
        assertEquals(10, a[0] + a[1] + a[2]);
    }
}
