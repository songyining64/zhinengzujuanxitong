package com.example.exam.module.question.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 试题查重：题干采用字符 bigram Jaccard；选项 JSON 解析后做文本集合 Jaccard。
 * 综合分 = 题干权重 0.6 + 选项权重 0.4（无选项时仅题干）。
 */
public final class QuestionSimilarityHelper {

    private static final ObjectMapper OM = new ObjectMapper();

    private QuestionSimilarityHelper() {
    }

    public static String normalizeStem(String stem) {
        if (stem == null) {
            return "";
        }
        String s = stem.trim().replaceAll("\\s+", " ");
        return s.toLowerCase(Locale.ROOT);
    }

    /**
     * 字符 bigram 的 Jaccard 相似度，适合中文题干。
     */
    public static double stemSimilarity(String a, String b) {
        String na = normalizeStem(a);
        String nb = normalizeStem(b);
        if (na.isEmpty() && nb.isEmpty()) {
            return 1.0;
        }
        if (na.isEmpty() || nb.isEmpty()) {
            return 0.0;
        }
        if (na.equals(nb)) {
            return 1.0;
        }
        if (na.length() < 2 || nb.length() < 2) {
            return na.equals(nb) ? 1.0 : 0.0;
        }
        Set<String> sa = bigrams(na);
        Set<String> sb = bigrams(nb);
        return jaccard(sa, sb);
    }

    private static Set<String> bigrams(String s) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i < s.length() - 1; i++) {
            set.add(s.substring(i, i + 2));
        }
        return set;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int inter = 0;
        for (String x : a) {
            if (b.contains(x)) {
                inter++;
            }
        }
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0.0 : (double) inter / union;
    }

    /**
     * 从 options JSON 抽取文本片段做集合 Jaccard。
     */
    public static double optionsSimilarity(String optionsJsonA, String optionsJsonB) {
        if (optionsJsonA == null) {
            optionsJsonA = "";
        }
        if (optionsJsonB == null) {
            optionsJsonB = "";
        }
        String ta = optionsJsonA.trim();
        String tb = optionsJsonB.trim();
        if (ta.isEmpty() && tb.isEmpty()) {
            return 1.0;
        }
        if (ta.isEmpty() || tb.isEmpty()) {
            return 0.0;
        }
        if (ta.equals(tb)) {
            return 1.0;
        }
        Set<String> sa = tokenizeOptions(ta);
        Set<String> sb = tokenizeOptions(tb);
        if (sa.isEmpty() && sb.isEmpty()) {
            return stemSimilarity(ta, tb);
        }
        return jaccard(sa, sb);
    }

    private static Set<String> tokenizeOptions(String json) {
        Set<String> out = new HashSet<>();
        try {
            JsonNode root = OM.readTree(json);
            collectText(root, out);
        } catch (Exception e) {
            out.add(normalizeStem(json));
        }
        return out;
    }

    private static void collectText(JsonNode n, Set<String> out) {
        if (n == null || n.isNull()) {
            return;
        }
        if (n.isTextual()) {
            String t = normalizeStem(n.asText());
            if (!t.isEmpty()) {
                out.add(t);
            }
            return;
        }
        if (n.isArray()) {
            for (JsonNode c : n) {
                collectText(c, out);
            }
            return;
        }
        if (n.isObject()) {
            Iterator<String> it = n.fieldNames();
            while (it.hasNext()) {
                collectText(n.get(it.next()), out);
            }
        }
    }

    /**
     * 综合相似度 [0,1]。
     */
    public static double combinedSimilarity(String stemA, String optA, String stemB, String optB) {
        double s = stemSimilarity(stemA, stemB);
        String oa = optA == null ? "" : optA.trim();
        String ob = optB == null ? "" : optB.trim();
        if (oa.isEmpty() && ob.isEmpty()) {
            return s;
        }
        double o = optionsSimilarity(optA, optB);
        return 0.6 * s + 0.4 * o;
    }
}
