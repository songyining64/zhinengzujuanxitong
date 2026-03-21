package com.example.exam.module.exam.util;

import com.example.exam.common.enums.QuestionTypeEnum;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.stream.Collectors;

public final class QuestionAnswerMatcher {

    private QuestionAnswerMatcher() {
    }

    public static boolean matches(String type, String standardAnswer, String userAnswer) {
        if (standardAnswer == null) {
            return false;
        }
        if (userAnswer == null) {
            userAnswer = "";
        }
        QuestionTypeEnum t;
        try {
            t = QuestionTypeEnum.valueOf(type);
        } catch (Exception e) {
            return false;
        }
        String s = standardAnswer.trim();
        String u = userAnswer.trim();
        return switch (t) {
            case SINGLE, TRUE_FALSE -> normalize(u).equals(normalize(s));
            case MULTIPLE -> normalizeMulti(u).equals(normalizeMulti(s));
            case FILL -> normalize(u).equalsIgnoreCase(normalize(s));
            case SHORT -> false;
        };
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeMulti(String raw) {
        String[] parts = raw.split("[,，;；\\s]+");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .map(x -> x.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted()
                .collect(Collectors.joining(","));
    }
}
