package com.example.exam.module.paper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.paper.dto.PaperAutoGenRequest;
import com.example.exam.module.paper.dto.PaperDetailVO;
import com.example.exam.module.paper.dto.PaperManualRequest;
import com.example.exam.module.paper.entity.Paper;
import com.example.exam.module.paper.entity.PaperQuestion;
import com.example.exam.module.paper.mapper.PaperMapper;
import com.example.exam.module.paper.mapper.PaperQuestionMapper;
import com.example.exam.module.paper.service.PaperService;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.mapper.QuestionMapper;
import com.example.exam.module.question.service.KnowledgePointService;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private static final String MODE_MANUAL = "MANUAL";
    private static final String MODE_AUTO = "AUTO";

    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgePointService knowledgePointService;
    private final CoursePermissionService coursePermissionService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

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
    public Paper generateAuto(PaperAutoGenRequest req) {
        coursePermissionService.assertCourseManageable(req.getCourseId());
        if (req.getCountByType() == null || req.getCountByType().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请配置各题型数量");
        }
        BigDecimal per = req.getScorePerQuestion() != null ? req.getScorePerQuestion() : BigDecimal.TEN;
        long seed = req.getRandomSeed() != null ? req.getRandomSeed() : System.nanoTime();
        Random random = new Random(seed);

        Set<Long> kpFilter = new HashSet<>();
        boolean includeDesc = Boolean.TRUE.equals(req.getIncludeKnowledgeDescendants());
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

        Set<Long> picked = new HashSet<>();
        List<PaperQuestion> buffer = new ArrayList<>();
        int order = 1;
        boolean dedup = !Boolean.FALSE.equals(req.getDedup());

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
                int[] alloc = allocateNeedByWeights(need, wE, wM, wH);
                String[] tierNames = {"EASY", "MEDIUM", "HARD"};
                for (int ti = 0; ti < 3; ti++) {
                    int tierNeed = alloc[ti];
                    if (tierNeed <= 0) {
                        continue;
                    }
                    int tier = ti + 1;
                    LambdaQueryWrapper<Question> wq = new LambdaQueryWrapper<Question>()
                            .eq(Question::getCourseId, req.getCourseId())
                            .eq(Question::getType, type)
                            .eq(Question::getStatus, 1)
                            .in(Question::getKnowledgePointId, kpFilter);
                    if (tier == 1) {
                        wq.and(x -> x.eq(Question::getDifficulty, 1).or().isNull(Question::getDifficulty));
                    } else {
                        wq.eq(Question::getDifficulty, tier);
                    }
                    List<Question> pool = questionMapper.selectList(wq);
                    List<Question> copy = new ArrayList<>(pool);
                    Collections.shuffle(copy, random);
                    int taken = 0;
                    for (Question q : copy) {
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
                        pq.setScore(per);
                        buffer.add(pq);
                        taken++;
                    }
                    if (taken < tierNeed) {
                        throw new BizException(ErrorCode.BAD_REQUEST,
                                "题库不足: 题型 " + type + " 难度 " + tierNames[ti] + " 需要 " + tierNeed + " 题，仅可选 " + taken);
                    }
                }
            } else {
                List<Question> pool = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                        .eq(Question::getCourseId, req.getCourseId())
                        .eq(Question::getType, type)
                        .eq(Question::getStatus, 1)
                        .in(Question::getKnowledgePointId, kpFilter));
                List<Question> copy = new ArrayList<>(pool);
                Collections.shuffle(copy, random);
                int taken = 0;
                for (Question q : copy) {
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
                    pq.setScore(per);
                    buffer.add(pq);
                    taken++;
                }
                if (taken < need) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "题库不足: 题型 " + type + " 需要 " + need + " 题，仅可选 " + taken);
                }
            }
        }

        BigDecimal total = per.multiply(BigDecimal.valueOf(buffer.size()));
        Paper paper = new Paper();
        paper.setCourseId(req.getCourseId());
        paper.setTitle(req.getTitle());
        paper.setMode(MODE_AUTO);
        paper.setTotalScore(total);
        paper.setRandomSeed(seed);
        paper.setCreatorId(me().getId());
        paper.setCreateTime(LocalDateTime.now());
        paper.setUpdateTime(LocalDateTime.now());
        try {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("knowledgePointIds", req.getKnowledgePointIds());
            snap.put("includeKnowledgeDescendants", req.getIncludeKnowledgeDescendants());
            snap.put("scorePerQuestion", req.getScorePerQuestion());
            snap.put("randomSeed", seed);
            snap.put("dedup", req.getDedup());
            snap.put("countByType", req.getCountByType());
            snap.put("difficultyWeights", req.getDifficultyWeights());
            paper.setRulesJson(objectMapper.writeValueAsString(snap));
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "规则序列化失败");
        }
        paperMapper.insert(paper);
        for (PaperQuestion pq : buffer) {
            pq.setPaperId(paper.getId());
            paperQuestionMapper.insert(pq);
        }
        return paperMapper.selectById(paper.getId());
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

    /** 按 EASY/MEDIUM/HARD 权重将 need 拆成三档题量 */
    private static int[] allocateNeedByWeights(int need, int w1, int w2, int w3) {
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
