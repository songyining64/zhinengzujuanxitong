package com.example.exam.module.exam.util;

import com.example.exam.common.enums.QuestionTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * 考试题目顺序与选项乱序（与答卷存储的「原始选项标号」对齐）。
 */
public final class ExamShuffleHelper {

    private ExamShuffleHelper() {
    }

    public static List<Long> shuffledQuestionIds(List<Long> baseOrder, long shuffleSeed) {
        List<Long> copy = new ArrayList<>(baseOrder);
        Collections.shuffle(copy, new Random(shuffleSeed));
        return copy;
    }

    public static boolean supportsOptionShuffle(String questionType) {
        if (questionType == null) {
            return false;
        }
        try {
            QuestionTypeEnum t = QuestionTypeEnum.valueOf(questionType);
            return t == QuestionTypeEnum.SINGLE
                    || t == QuestionTypeEnum.MULTIPLE
                    || t == QuestionTypeEnum.TRUE_FALSE;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将选项 JSON 乱序后返回展示用 JSON（数组字符串）。
     */
    public static String shuffleOptionsJson(String optionsJson, long shuffleSeed, long questionId, ObjectMapper om) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return optionsJson;
        }
        try {
            JsonNode root = om.readTree(optionsJson);
            if (!root.isArray() || root.size() <= 1) {
                return optionsJson;
            }
            List<JsonNode> nodes = new ArrayList<>();
            for (JsonNode n : root) {
                nodes.add(n);
            }
            List<Integer> perm = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i++) {
                perm.add(i);
            }
            Collections.shuffle(perm, new Random(shuffleSeed ^ questionId));
            ArrayNode out = om.createArrayNode();
            for (int idx : perm) {
                out.add(nodes.get(idx));
            }
            return om.writeValueAsString(out);
        } catch (Exception e) {
            return optionsJson;
        }
    }

    /**
     * 学生提交的为「展示标号」（乱序后 A/B/C…），转换为题库中的原始标号再判分/入库。
     */
    public static String displayAnswerToOriginal(String optionsJson, long shuffleSeed, long questionId,
                                                 String displayAnswer, ObjectMapper om) {
        if (displayAnswer == null) {
            return null;
        }
        String trimmed = displayAnswer.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (optionsJson == null || optionsJson.isBlank()) {
            return displayAnswer;
        }
        try {
            JsonNode root = om.readTree(optionsJson);
            if (!root.isArray() || root.size() <= 1) {
                return displayAnswer;
            }
            int n = root.size();
            List<Integer> perm = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                perm.add(i);
            }
            Collections.shuffle(perm, new Random(shuffleSeed ^ questionId));
            // display position k -> original index = perm[k]
            String upper = trimmed.toUpperCase(Locale.ROOT);
            if (upper.contains(",") || upper.contains("，") || upper.contains(";") || upper.contains("；")) {
                String[] parts = upper.split("[,，;；\\s]+");
                List<String> origLetters = new ArrayList<>();
                for (String p : parts) {
                    p = p.trim();
                    if (p.isEmpty()) {
                        continue;
                    }
                    origLetters.add(singleDisplayToOriginal(perm, p));
                }
                origLetters.sort(String::compareTo);
                return String.join(",", origLetters);
            }
            return singleDisplayToOriginal(perm, upper);
        } catch (Exception e) {
            return displayAnswer;
        }
    }

    /**
     * 将已保存的原始标号转为当前展示标号（与 {@link #displayAnswerToOriginal} 互逆）。
     */
    public static String originalAnswerToDisplay(String optionsJson, long shuffleSeed, long questionId,
                                                 String originalAnswer, ObjectMapper om) {
        if (originalAnswer == null) {
            return null;
        }
        String trimmed = originalAnswer.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (optionsJson == null || optionsJson.isBlank()) {
            return originalAnswer;
        }
        try {
            JsonNode root = om.readTree(optionsJson);
            if (!root.isArray() || root.size() <= 1) {
                return originalAnswer;
            }
            int n = root.size();
            List<Integer> perm = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                perm.add(i);
            }
            Collections.shuffle(perm, new Random(shuffleSeed ^ questionId));
            String upper = trimmed.toUpperCase(Locale.ROOT);
            if (upper.contains(",") || upper.contains("，") || upper.contains(";") || upper.contains("；")) {
                String[] parts = upper.split("[,，;；\\s]+");
                List<String> dispLetters = new ArrayList<>();
                for (String p : parts) {
                    p = p.trim();
                    if (p.isEmpty()) {
                        continue;
                    }
                    dispLetters.add(singleOriginalToDisplay(perm, p));
                }
                dispLetters.sort(String::compareTo);
                return String.join(",", dispLetters);
            }
            return singleOriginalToDisplay(perm, upper);
        } catch (Exception e) {
            return originalAnswer;
        }
    }

    private static String singleOriginalToDisplay(List<Integer> perm, String oneLetter) {
        if (oneLetter.isEmpty()) {
            return oneLetter;
        }
        char c = oneLetter.charAt(0);
        if (c < 'A' || c > 'Z') {
            return oneLetter;
        }
        int origIdx = c - 'A';
        if (origIdx < 0 || origIdx >= perm.size()) {
            return oneLetter;
        }
        for (int k = 0; k < perm.size(); k++) {
            if (perm.get(k) == origIdx) {
                return String.valueOf((char) ('A' + k));
            }
        }
        return oneLetter;
    }

    private static String singleDisplayToOriginal(List<Integer> perm, String oneLetter) {
        if (oneLetter.isEmpty()) {
            return oneLetter;
        }
        char c = oneLetter.charAt(0);
        if (c < 'A' || c > 'Z') {
            return oneLetter;
        }
        int disp = c - 'A';
        if (disp < 0 || disp >= perm.size()) {
            return oneLetter;
        }
        int origIdx = perm.get(disp);
        return String.valueOf((char) ('A' + origIdx));
    }
}
