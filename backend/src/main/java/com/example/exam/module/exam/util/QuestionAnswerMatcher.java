package com.example.exam.module.exam.util;

import com.example.exam.common.enums.QuestionTypeEnum;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        // 兼容 A,B / A B / AB / A、B / [A,B] 等输入，统一成去重升序集合
        String cleaned = raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", " ");
        String[] parts = cleaned.split("\\s+");
        Pattern letterPattern = Pattern.compile("[A-H]");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .flatMap(token -> {
                    LinkedHashSet<String> letters = new LinkedHashSet<>();
                    Matcher matcher = letterPattern.matcher(token);
                    while (matcher.find()) {
                        letters.add(matcher.group());
                    }
                    return letters.stream();
                })
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted()
                .collect(Collectors.joining(","));
    }
}
