package com.example.exam.module.paper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.QuestionReviewStatusEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.config.ExamProperties;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.paper.dto.PaperAutoGenRequest;
import com.example.exam.module.paper.dto.PaperAutoGenResult;
import com.example.exam.module.paper.dto.PaperDetailVO;
import com.example.exam.module.paper.dto.PaperFromTemplateRequest;
import com.example.exam.module.paper.dto.PaperManualRequest;
import com.example.exam.module.paper.entity.Paper;
import com.example.exam.module.paper.entity.PaperGenerationLog;
import com.example.exam.module.paper.entity.PaperQuestion;
import com.example.exam.module.paper.entity.PaperTemplate;
import com.example.exam.module.paper.mapper.PaperGenerationLogMapper;
import com.example.exam.module.paper.mapper.PaperMapper;
import com.example.exam.module.paper.mapper.PaperQuestionMapper;
import com.example.exam.module.paper.mapper.PaperTemplateMapper;
import com.example.exam.module.paper.service.PaperService;
import com.example.exam.module.paper.support.PaperDifficultyAllocator;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.mapper.QuestionMapper;
import com.example.exam.module.question.service.KnowledgePointService;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import com.example.exam.module.wrongbook.entity.WrongBook;
import com.example.exam.module.wrongbook.mapper.WrongBookMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private static final String MODE_MANUAL = "MANUAL";
    private static final String MODE_AUTO = "AUTO";
    private static final String LOG_MODE_AUTO = "AUTO";
    private static final String LOG_MODE_TEMPLATE = "TEMPLATE";

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgePointService knowledgePointService;
    private final CoursePermissionService coursePermissionService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final PaperGenerationLogMapper paperGenerationLogMapper;
    private final PaperTemplateMapper paperTemplateMapper;
    private final WrongBookMapper wrongBookMapper;
    private final ExamProperties examProperties;

    private User me() {
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return u;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Paper createManual(PaperManualRequest req) {
        coursePermissionService.assertCourseManageable(req.getCourseId());
        BigDecimal total = req.getItems().stream()
                .map(PaperManualRequest.PaperManualItem::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Paper paper = new Paper();
        paper.setCourseId(req.getCourseId());
        paper.setTitle(req.getTitle());
        paper.setMode(MODE_MANUAL);
        paper.setTotalScore(total);
        paper.setCreatorId(me().getId());
        paper.setCreateTime(LocalDateTime.now());
        paper.setUpdateTime(LocalDateTime.now());
        paperMapper.insert(paper);
        int order = 1;
        for (PaperManualRequest.PaperManualItem it : req.getItems()) {
            Question q = questionMapper.selectById(it.getQuestionId());
            if (q == null || !q.getCourseId().equals(req.getCourseId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "试题不存在或不属于该课程");
            }
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(it.getQuestionId());
            pq.setQuestionOrder(order++);
            pq.setScore(it.getScore());
            paperQuestionMapper.insert(pq);
        }
        return paperMapper.selectById(paper.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperAutoGenResult generateAuto(PaperAutoGenRequest req) {
        long t0 = System.currentTimeMillis();
        PaperAutoGenResult result = doGenerateAuto(req);
        long ms = System.currentTimeMillis() - t0;
        result.setDurationMs(ms);
        if (ms > examProperties.getPaperCompose().getComposeTimeoutMs()) {
            result.getWarnings().add("组卷耗时超过配置阈值（"
                    + examProperties.getPaperCompose().getComposeTimeoutMs()
                    + "ms），若约束较多可考虑缩小知识点范围或题量。");
            result.setPartialConstraint(true);
        }
        saveGenerationLog(result.getPaper(), req, LOG_MODE_AUTO, ms, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperAutoGenResult generateFromTemplate(Long templateId, PaperFromTemplateRequest req) {
        PaperTemplate t = paperTemplateMapper.selectById(templateId);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "组卷模板不存在");
        }
        coursePermissionService.assertCourseManageable(t.getCourseId());
        PaperAutoGenRequest rules;
        try {
            rules = objectMapper.readValue(t.getRulesJson(), PaperAutoGenRequest.class);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "模板规则解析失败");
        }
        rules.setCourseId(t.getCourseId());
        rules.setTitle(req.getTitle());
        rules.setRandomSeed(req.getRandomSeed());
        long t0 = System.currentTimeMillis();
        PaperAutoGenResult result = doGenerateAuto(rules);
        long ms = System.currentTimeMillis() - t0;
        result.setDurationMs(ms);
        saveGenerationLog(result.getPaper(), rules, LOG_MODE_TEMPLATE, ms, result);
        return result;
    }

    private void saveGenerationLog(Paper paper, PaperAutoGenRequest req, String mode, long durationMs,
                                   PaperAutoGenResult meta) {
        PaperGenerationLog log = new PaperGenerationLog();
        log.setPaperId(paper.getId());
        log.setCourseId(paper.getCourseId());
        log.setOperatorId(me().getId());
        log.setMode(mode);
        try {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("knowledgePointIds", req.getKnowledgePointIds());
            snap.put("randomPool", req.getRandomPool());
            snap.put("fixedQuestionIds", req.getFixedQuestionIds());
            snap.put("includeKnowledgeDescendants", req.getIncludeKnowledgeDescendants());
            snap.put("scorePerQuestion", req.getScorePerQuestion());
            snap.put("scoreByType", req.getScoreByType());
            snap.put("targetTotalScore", req.getTargetTotalScore());
            snap.put("minKnowledgeCoverage", req.getMinKnowledgeCoverage());
            snap.put("allowPartialConstraints", req.getAllowPartialConstraints());
            snap.put("excludeQuestionIds", req.getExcludeQuestionIds());
            snap.put("forbiddenQuestionIds", req.getForbiddenQuestionIds());
            snap.put("excludeWrongBookForStudentId", req.getExcludeWrongBookForStudentId());
            snap.put("randomSeed", paper.getRandomSeed());
            snap.put("dedup", req.getDedup());
            snap.put("countByType", req.getCountByType());
            snap.put("difficultyWeights", req.getDifficultyWeights());
            snap.put("algorithmMode", meta != null ? meta.getAlgorithmMode() : req.getAlgorithmMode());
            if (meta != null) {
                snap.put("knowledgeCoverage", meta.getKnowledgeCoverage());
                snap.put("partialConstraint", meta.isPartialConstraint());
                snap.put("warnings", meta.getWarnings());
                snap.put("suggestions", meta.getSuggestions());
                snap.put("durationMs", meta.getDurationMs());
            }
            log.setRulesJson(objectMapper.writeValueAsString(snap));
        } catch (JsonProcessingException ex) {
            log.setRulesJson(null);
        }
        log.setDurationMs((int) Math.min(durationMs, Integer.MAX_VALUE));
        log.setCreateTime(LocalDateTime.now());
        paperGenerationLogMapper.insert(log);
    }

    private PaperAutoGenResult doGenerateAuto(PaperAutoGenRequest req) {
        coursePermissionService.assertCourseManageable(req.getCourseId());
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请填写试卷标题");
        }
        if (req.getCountByType() == null || req.getCountByType().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请配置各题型数量");
        }
        boolean randomPool = Boolean.TRUE.equals(req.getRandomPool());
        if (!randomPool) {
            if (req.getKnowledgePointIds() == null || req.getKnowledgePointIds().isEmpty()) {
                throw new BizException(ErrorCode.BAD_REQUEST, "请指定知识点或开启随机全课程抽题");
            }
        }

        PaperAutoGenResult result = new PaperAutoGenResult();
        String algo = req.getAlgorithmMode();
        if (algo == null || algo.isBlank()) {
            algo = examProperties.getPaperCompose().getDefaultAlgorithm();
        }
        if ("GENETIC".equalsIgnoreCase(algo) || "SA".equalsIgnoreCase(algo)) {
            result.getWarnings().add("遗传/模拟退火算法尚未启用，本次已使用贪心多约束组卷。");
            algo = "GREEDY";
        }
        result.setAlgorithmMode(algo);

        BigDecimal defaultPer = req.getScorePerQuestion() != null ? req.getScorePerQuestion() : BigDecimal.TEN;
        long seed = req.getRandomSeed() != null ? req.getRandomSeed() : System.nanoTime();
        Random random = new Random(seed);

        Set<Long> kpFilter = new HashSet<>();
        boolean includeDesc = Boolean.TRUE.equals(req.getIncludeKnowledgeDescendants());
        if (!randomPool) {
            for (Long kpid : req.getKnowledgePointIds()) {
                if (includeDesc) {
                    kpFilter.addAll(knowledgePointService.listSelfAndDescendantIds(kpid));
                } else {
                    kpFilter.add(kpid);
                }
            }
            if (kpFilter.isEmpty()) {
                throw new BizException(ErrorCode.BAD_REQUEST, "知识点无效");
            }
        }

        Set<Long> globalExclude = buildGlobalExclude(req);

        Set<Long> picked = new HashSet<>();
        List<PaperQuestion> buffer = new ArrayList<>();
        int order = 1;
        boolean dedup = !Boolean.FALSE.equals(req.getDedup());

        Set<Long> uncovered = (!randomPool && !kpFilter.isEmpty()) ? new HashSet<>(kpFilter) : null;

        int fixedCount = appendFixedQuestions(req, defaultPer, picked, buffer, order, dedup, uncovered, kpFilter, randomPool);
        order = buffer.size() + 1;

        Map<String, Integer> dw = req.getDifficultyWeights();
        int wE = dw != null ? dw.getOrDefault("EASY", 0) : 0;
        int wM = dw != null ? dw.getOrDefault("MEDIUM", 0) : 0;
        int wH = dw != null ? dw.getOrDefault("HARD", 0) : 0;
        boolean useDifficultyWeights = dw != null && !dw.isEmpty() && (wE + wM + wH) > 0;

        for (Map.Entry<String, Integer> e : req.getCountByType().entrySet()) {
            String type = e.getKey();
            int need = e.getValue() == null ? 0 : e.getValue();
            if (need <= 0) {
                continue;
            }
            if (useDifficultyWeights) {
                int[] alloc = PaperDifficultyAllocator.allocateNeedByWeights(need, wE, wM, wH);
                String[] tierNames = {"EASY", "MEDIUM", "HARD"};
                for (int ti = 0; ti < 3; ti++) {
                    int tierNeed = alloc[ti];
                    if (tierNeed <= 0) {
                        continue;
                    }
                    int tier = ti + 1;
                    List<Question> pool = loadPool(req.getCourseId(), type, kpFilter, randomPool, tier, globalExclude);
                    prioritizePoolForCoverage(pool, uncovered, random);
                    int taken = 0;
                    for (Question q : pool) {
                        if (taken >= tierNeed) {
                            break;
                        }
                        if (dedup && picked.contains(q.getId())) {
                            continue;
                        }
                        picked.add(q.getId());
                        PaperQuestion pq = new PaperQuestion();
                        pq.setQuestionId(q.getId());
                        pq.setQuestionOrder(order++);
                        pq.setScore(scoreForType(req, type, defaultPer));
                        buffer.add(pq);
                        taken++;
                        if (uncovered != null && kpFilter.contains(q.getKnowledgePointId())) {
                            uncovered.remove(q.getKnowledgePointId());
                        }
                    }
                    if (taken < tierNeed) {
                        throw new BizException(ErrorCode.BAD_REQUEST,
                                "题库不足: 题型 " + type + " 难度 " + tierNames[ti] + " 需要 " + tierNeed + " 题，仅可选 " + taken
                                        + "。可尝试关闭错题过滤、缩小难度权重或补充该知识点试题。");
                    }
                }
            } else {
                List<Question> pool = loadPool(req.getCourseId(), type, kpFilter, randomPool, null, globalExclude);
                prioritizePoolForCoverage(pool, uncovered, random);
                int taken = 0;
                for (Question q : pool) {
                    if (taken >= need) {
                        break;
                    }
                    if (dedup && picked.contains(q.getId())) {
                        continue;
                    }
                    picked.add(q.getId());
                    PaperQuestion pq = new PaperQuestion();
                    pq.setQuestionId(q.getId());
                    pq.setQuestionOrder(order++);
                    pq.setScore(scoreForType(req, type, defaultPer));
                    buffer.add(pq);
                    taken++;
                    if (uncovered != null && kpFilter.contains(q.getKnowledgePointId())) {
                        uncovered.remove(q.getKnowledgePointId());
                    }
                }
                if (taken < need) {
                    throw new BizException(ErrorCode.BAD_REQUEST,
                            "题库不足: 题型 " + type + " 需要 " + need + " 题，仅可选 " + taken + "。");
                }
            }
        }

        int swaps = repairKnowledgeCoverage(req, buffer, fixedCount, globalExclude, random, kpFilter, randomPool,
                examProperties.getPaperCompose().getGreedyMaxRepairSwaps(), defaultPer);
        if (swaps > 0) {
            result.getWarnings().add("已进行 " + swaps + " 次贪心替换以提升知识点覆盖。");
        }

        BigDecimal coverage;
        if (randomPool || kpFilter.isEmpty()) {
            coverage = BigDecimal.ONE;
        } else {
            coverage = computeKnowledgeCoverage(buffer, kpFilter);
        }
        result.setKnowledgeCoverage(coverage);

        BigDecimal totalScore = buffer.stream()
                .map(PaperQuestion::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (req.getTargetTotalScore() != null) {
            BigDecimal delta = totalScore.subtract(req.getTargetTotalScore()).abs();
            if (delta.compareTo(new BigDecimal("0.02")) > 0) {
                if (!Boolean.TRUE.equals(req.getAllowPartialConstraints())) {
                    throw new BizException(ErrorCode.BAD_REQUEST,
                            "试卷总分 " + totalScore + " 与目标总分 " + req.getTargetTotalScore()
                                    + " 不一致，请调整 scoreByType / 题量，或开启允许放宽约束。");
                }
                result.getWarnings().add("总分与目标不符：实际 " + totalScore + "，目标 " + req.getTargetTotalScore());
                result.setPartialConstraint(true);
            }
        }

        if (req.getMinKnowledgeCoverage() != null && !randomPool && !kpFilter.isEmpty()) {
            BigDecimal min = req.getMinKnowledgeCoverage().min(BigDecimal.ONE).max(BigDecimal.ZERO);
            if (coverage.compareTo(min) < 0) {
                fillCoverageSuggestions(result, coverage, min);
                if (!Boolean.TRUE.equals(req.getAllowPartialConstraints())) {
                    String msg = String.format(
                            "知识点覆盖率 %.2f 低于要求 %.2f。%s",
                            coverage.doubleValue(), min.doubleValue(),
                            String.join("；", result.getSuggestions()));
                    throw new BizException(ErrorCode.BAD_REQUEST, msg);
                }
                result.getWarnings().addAll(result.getSuggestions());
                result.setPartialConstraint(true);
            }
        }

        Paper paper = new Paper();
        paper.setCourseId(req.getCourseId());
        paper.setTitle(req.getTitle());
        paper.setMode(MODE_AUTO);
        paper.setTotalScore(totalScore);
        paper.setRandomSeed(seed);
        paper.setCreatorId(me().getId());
        paper.setCreateTime(LocalDateTime.now());
        paper.setUpdateTime(LocalDateTime.now());
        try {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("knowledgePointIds", req.getKnowledgePointIds());
            snap.put("randomPool", req.getRandomPool());
            snap.put("fixedQuestionIds", req.getFixedQuestionIds());
            snap.put("includeKnowledgeDescendants", req.getIncludeKnowledgeDescendants());
            snap.put("scorePerQuestion", req.getScorePerQuestion());
            snap.put("scoreByType", req.getScoreByType());
            snap.put("targetTotalScore", req.getTargetTotalScore());
            snap.put("minKnowledgeCoverage", req.getMinKnowledgeCoverage());
            snap.put("allowPartialConstraints", req.getAllowPartialConstraints());
            snap.put("excludeQuestionIds", req.getExcludeQuestionIds());
            snap.put("forbiddenQuestionIds", req.getForbiddenQuestionIds());
            snap.put("excludeWrongBookForStudentId", req.getExcludeWrongBookForStudentId());
            snap.put("randomSeed", seed);
            snap.put("dedup", req.getDedup());
            snap.put("countByType", req.getCountByType());
            snap.put("difficultyWeights", req.getDifficultyWeights());
            snap.put("knowledgeCoverage", coverage);
            snap.put("warnings", result.getWarnings());
            snap.put("partialConstraint", result.isPartialConstraint());
            snap.put("algorithmMode", result.getAlgorithmMode());
            paper.setRulesJson(objectMapper.writeValueAsString(snap));
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "规则序列化失败");
        }
        paperMapper.insert(paper);
        for (PaperQuestion pq : buffer) {
            pq.setPaperId(paper.getId());
            paperQuestionMapper.insert(pq);
        }
        result.setPaper(paperMapper.selectById(paper.getId()));
        return result;
    }

    private void fillCoverageSuggestions(PaperAutoGenResult result, BigDecimal coverage, BigDecimal min) {
        result.getSuggestions().clear();
        result.getSuggestions().add(String.format(
                "当前知识点覆盖率约 %s，低于要求 %s。",
                coverage.setScale(4, RoundingMode.HALF_UP).toPlainString(),
                min.setScale(4, RoundingMode.HALF_UP).toPlainString()));
        result.getSuggestions().add("可将「最低覆盖率」下调后重试，或补充未覆盖知识点下的已发布试题，或关闭「错题/禁选」过滤以增加可用题池。");
    }

    private BigDecimal computeKnowledgeCoverage(List<PaperQuestion> buffer, Set<Long> kpFilter) {
        if (kpFilter.isEmpty()) {
            return BigDecimal.ONE;
        }
        Set<Long> hit = new HashSet<>();
        for (PaperQuestion pq : buffer) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q != null && kpFilter.contains(q.getKnowledgePointId())) {
                hit.add(q.getKnowledgePointId());
            }
        }
        return BigDecimal.valueOf(hit.size())
                .divide(BigDecimal.valueOf(kpFilter.size()), 4, RoundingMode.HALF_UP);
    }

    private int repairKnowledgeCoverage(PaperAutoGenRequest req, List<PaperQuestion> buffer, int fixedSize,
                                        Set<Long> globalExclude, Random random, Set<Long> kpFilter, boolean randomPool,
                                        int maxSwaps, BigDecimal defaultPer) {
        if (randomPool || kpFilter.isEmpty() || maxSwaps <= 0) {
            return 0;
        }
        int swaps = 0;
        while (swaps < maxSwaps) {
            Set<Long> uncovered = computeUncoveredKps(buffer, kpFilter);
            if (uncovered.isEmpty()) {
                break;
            }
            Long targetKp = uncovered.iterator().next();
            boolean progressed = false;
            for (int i = fixedSize; i < buffer.size() && swaps < maxSwaps; i++) {
                PaperQuestion pq = buffer.get(i);
                Question cur = questionMapper.selectById(pq.getQuestionId());
                if (cur == null) {
                    continue;
                }
                List<Question> candidates = loadReplacementCandidates(
                        req.getCourseId(), cur.getType(), targetKp, globalExclude, buffer, pq.getQuestionId());
                if (candidates.isEmpty()) {
                    continue;
                }
                Collections.shuffle(candidates, random);
                Question neu = candidates.get(0);
                pq.setQuestionId(neu.getId());
                pq.setScore(scoreForType(req, neu.getType(), defaultPer));
                swaps++;
                progressed = true;
                break;
            }
            if (!progressed) {
                break;
            }
        }
        return swaps;
    }

    private Set<Long> computeUncoveredKps(List<PaperQuestion> buffer, Set<Long> kpFilter) {
        Set<Long> hit = new HashSet<>();
        for (PaperQuestion pq : buffer) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q != null && kpFilter.contains(q.getKnowledgePointId())) {
                hit.add(q.getKnowledgePointId());
            }
        }
        Set<Long> uncovered = new HashSet<>(kpFilter);
        uncovered.removeAll(hit);
        return uncovered;
    }

    private List<Question> loadReplacementCandidates(Long courseId, String type, Long knowledgePointId,
                                                     Set<Long> globalExclude, List<PaperQuestion> buffer,
                                                     Long currentQuestionId) {
        Set<Long> pickedOther = buffer.stream()
                .map(PaperQuestion::getQuestionId)
                .filter(id -> !id.equals(currentQuestionId))
                .collect(Collectors.toSet());
        LambdaQueryWrapper<Question> wq = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .eq(Question::getType, type)
                .eq(Question::getKnowledgePointId, knowledgePointId)
                .eq(Question::getStatus, 1);
        addPublishedFilter(wq);
        if (globalExclude != null && !globalExclude.isEmpty()) {
            wq.notIn(Question::getId, globalExclude);
        }
        if (!pickedOther.isEmpty()) {
            wq.notIn(Question::getId, pickedOther);
        }
        return questionMapper.selectList(wq);
    }

    private void prioritizePoolForCoverage(List<Question> pool, Set<Long> uncovered, Random random) {
        if (uncovered == null || uncovered.isEmpty()) {
            Collections.shuffle(pool, random);
            return;
        }
        List<Question> pri = new ArrayList<>();
        List<Question> rest = new ArrayList<>();
        for (Question q : pool) {
            if (uncovered.contains(q.getKnowledgePointId())) {
                pri.add(q);
            } else {
                rest.add(q);
            }
        }
        Collections.shuffle(pri, random);
        Collections.shuffle(rest, random);
        pool.clear();
        pool.addAll(pri);
        pool.addAll(rest);
    }

    private Set<Long> buildGlobalExclude(PaperAutoGenRequest req) {
        Set<Long> s = new HashSet<>();
        addAll(s, req.getExcludeQuestionIds());
        addAll(s, req.getForbiddenQuestionIds());
        Long sid = req.getExcludeWrongBookForStudentId();
        if (sid != null) {
            List<WrongBook> wb = wrongBookMapper.selectList(new LambdaQueryWrapper<WrongBook>()
                    .eq(WrongBook::getStudentId, sid)
                    .eq(WrongBook::getCourseId, req.getCourseId()));
            for (WrongBook w : wb) {
                if (w.getQuestionId() != null) {
                    s.add(w.getQuestionId());
                }
            }
        }
        return s;
    }

    private void addAll(Set<Long> target, List<Long> ids) {
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                target.add(id);
            }
        }
    }

    private BigDecimal scoreForType(PaperAutoGenRequest req, String type, BigDecimal defaultPer) {
        if (req.getScoreByType() != null && req.getScoreByType().containsKey(type)) {
            BigDecimal v = req.getScoreByType().get(type);
            return v != null ? v : defaultPer;
        }
        return defaultPer;
    }

    private int appendFixedQuestions(PaperAutoGenRequest req, BigDecimal defaultPer, Set<Long> picked,
                                     List<PaperQuestion> buffer, int startOrder, boolean dedup,
                                     Set<Long> uncovered, Set<Long> kpFilter, boolean randomPool) {
        if (req.getFixedQuestionIds() == null || req.getFixedQuestionIds().isEmpty()) {
            return 0;
        }
        Set<Long> seenFixed = new HashSet<>();
        int order = startOrder;
        int n = 0;
        for (Long qid : req.getFixedQuestionIds()) {
            if (qid == null) {
                continue;
            }
            if (seenFixed.contains(qid)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "固定试题列表存在重复 ID: " + qid);
            }
            seenFixed.add(qid);
            Question q = questionMapper.selectById(qid);
            if (q == null || !q.getCourseId().equals(req.getCourseId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "固定试题不存在或不属于该课程: " + qid);
            }
            if (q.getStatus() == null || q.getStatus() != 1) {
                throw new BizException(ErrorCode.BAD_REQUEST, "固定试题未启用: " + qid);
            }
            assertPublishedForPool(q);
            if (dedup && picked.contains(q.getId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "固定试题与已选题目冲突: " + qid);
            }
            picked.add(q.getId());
            PaperQuestion pq = new PaperQuestion();
            pq.setQuestionId(q.getId());
            pq.setQuestionOrder(order++);
            pq.setScore(scoreForType(req, q.getType(), defaultPer));
            buffer.add(pq);
            n++;
            if (!randomPool && uncovered != null && kpFilter != null && kpFilter.contains(q.getKnowledgePointId())) {
                uncovered.remove(q.getKnowledgePointId());
            }
        }
        return n;
    }

    private void assertPublishedForPool(Question q) {
        String rs = q.getReviewStatus();
        if (rs != null && !QuestionReviewStatusEnum.PUBLISHED.name().equals(rs)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "试题未审核通过，不能参与组卷: " + q.getId());
        }
    }

    private void addPublishedFilter(LambdaQueryWrapper<Question> wq) {
        wq.and(x -> x.eq(Question::getReviewStatus, QuestionReviewStatusEnum.PUBLISHED.name())
                .or().isNull(Question::getReviewStatus));
    }

    private List<Question> loadPool(Long courseId, String type, Set<Long> kpFilter, boolean randomPool,
                                    Integer difficultyTier, Set<Long> excludeIds) {
        LambdaQueryWrapper<Question> wq = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .eq(Question::getType, type)
                .eq(Question::getStatus, 1);
        addPublishedFilter(wq);
        if (!randomPool) {
            wq.in(Question::getKnowledgePointId, kpFilter);
        }
        if (excludeIds != null && !excludeIds.isEmpty()) {
            wq.notIn(Question::getId, excludeIds);
        }
        if (difficultyTier != null) {
            if (difficultyTier == 1) {
                wq.and(x -> x.eq(Question::getDifficulty, 1).or().isNull(Question::getDifficulty));
            } else {
                wq.eq(Question::getDifficulty, difficultyTier);
            }
        }
        return questionMapper.selectList(wq);
    }

    @Override
    public Page<PaperGenerationLog> pageGenerationLogs(Long courseId, long page, long size) {
        coursePermissionService.assertCourseReadable(courseId);
        LambdaQueryWrapper<PaperGenerationLog> w = new LambdaQueryWrapper<PaperGenerationLog>()
                .eq(PaperGenerationLog::getCourseId, courseId);
        Page<PaperGenerationLog> p = new Page<>(page, size);
        paperGenerationLogMapper.selectPage(p, w.orderByDesc(PaperGenerationLog::getCreateTime));
        return p;
    }

    @Override
    public Page<Paper> page(Long courseId, long page, long size) {
        coursePermissionService.assertCourseReadable(courseId);
        LambdaQueryWrapper<Paper> w = new LambdaQueryWrapper<Paper>().eq(Paper::getCourseId, courseId);
        Page<Paper> p = new Page<>(page, size);
        paperMapper.selectPage(p, w.orderByDesc(Paper::getCreateTime));
        return p;
    }

    @Override
    public PaperDetailVO getDetail(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        coursePermissionService.assertCourseReadable(paper.getCourseId());
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, id)
                .orderByAsc(PaperQuestion::getQuestionOrder));
        PaperDetailVO vo = new PaperDetailVO();
        vo.setPaper(paper);
        List<PaperDetailVO.PaperQuestionLine> lines = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            PaperDetailVO.PaperQuestionLine line = new PaperDetailVO.PaperQuestionLine();
            line.setPaperQuestionId(pq.getId());
            line.setQuestionId(pq.getQuestionId());
            line.setQuestionOrder(pq.getQuestionOrder());
            line.setScore(pq.getScore());
            if (q != null) {
                line.setType(q.getType());
                line.setStem(q.getStem());
            }
            lines.add(line);
        }
        vo.setQuestions(lines);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            return;
        }
        coursePermissionService.assertCourseManageable(paper.getCourseId());
        paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, id));
        paperMapper.deleteById(id);
    }
}
