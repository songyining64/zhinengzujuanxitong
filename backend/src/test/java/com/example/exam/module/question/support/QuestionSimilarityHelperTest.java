package com.example.exam.module.question.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionSimilarityHelperTest {

    @Test
    void stem_identical() {
        assertEquals(1.0, QuestionSimilarityHelper.stemSimilarity("下列说法正确的是", "下列说法正确的是"), 1e-6);
    }

    @Test
    void stem_similar() {
        double s = QuestionSimilarityHelper.stemSimilarity("下列说法正确的是", "下列说法正确的是（ ）");
        assertTrue(s > 0.5, "similar stems should score high: " + s);
    }

    @Test
    void options_json_array() {
        String a = "[\"A.1\",\"B.2\"]";
        String b = "[\"A.1\",\"B.2\"]";
        assertEquals(1.0, QuestionSimilarityHelper.optionsSimilarity(a, b), 1e-6);
    }
}
