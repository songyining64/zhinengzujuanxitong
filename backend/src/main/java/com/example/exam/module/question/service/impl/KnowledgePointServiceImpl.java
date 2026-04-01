package com.example.exam.module.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.exception.BizException;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.question.dto.KnowledgePointCreateRequest;
import com.example.exam.module.question.dto.KnowledgePointUpdateRequest;
import com.example.exam.module.question.entity.KnowledgePoint;
import com.example.exam.module.question.mapper.KnowledgePointMapper;
import com.example.exam.module.question.service.KnowledgePointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final KnowledgePointMapper knowledgePointMapper;
    private final CoursePermissionService coursePermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePoint create(KnowledgePointCreateRequest req) {
        coursePermissionService.assertCourseManageable(req.getCourseId());
        KnowledgePoint kp = new KnowledgePoint();
        kp.setCourseId(req.getCourseId());
        kp.setParentId(req.getParentId());
        kp.setName(req.getName());
        kp.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        kp.setCreateTime(LocalDateTime.now());
        knowledgePointMapper.insert(kp);
        return kp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePoint update(Long id, KnowledgePointUpdateRequest req) {
        KnowledgePoint kp = knowledgePointMapper.selectById(id);
        if (kp == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识点不存在");
        }
        coursePermissionService.assertCourseManageable(kp.getCourseId());
        if (req.getParentId() != null) {
            kp.setParentId(req.getParentId());
        }
        if (req.getName() != null) {
            kp.setName(req.getName());
        }
        if (req.getSortOrder() != null) {
            kp.setSortOrder(req.getSortOrder());
        }
        knowledgePointMapper.updateById(kp);
        return kp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KnowledgePoint kp = knowledgePointMapper.selectById(id);
        if (kp == null) {
            return;
        }
        coursePermissionService.assertCourseManageable(kp.getCourseId());
        List<Long> all = new ArrayList<>(listSelfAndDescendantIds(id));
        Collections.reverse(all);
        for (Long delId : all) {
            knowledgePointMapper.deleteById(delId);
        }
    }

    @Override
    public List<KnowledgePoint> listByCourse(Long courseId) {
        coursePermissionService.assertCourseReadable(courseId);
        return knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getCourseId, courseId)
                .orderByAsc(KnowledgePoint::getSortOrder)
                .orderByAsc(KnowledgePoint::getId));
    }

    @Override
    public List<Long> listSelfAndDescendantIds(Long knowledgePointId) {
        KnowledgePoint root = knowledgePointMapper.selectById(knowledgePointId);
        if (root == null) {
            return List.of();
        }
        List<KnowledgePoint> all = knowledgePointMapper.selectList(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getCourseId, root.getCourseId()));
        Map<Long, List<Long>> children = new HashMap<>();
        for (KnowledgePoint kp : all) {
            Long pid = kp.getParentId();
            children.computeIfAbsent(pid, k -> new ArrayList<>()).add(kp.getId());
        }
        List<Long> out = new ArrayList<>();
        ArrayDeque<Long> dq = new ArrayDeque<>();
        dq.add(root.getId());
        while (!dq.isEmpty()) {
            Long id = dq.removeFirst();
            out.add(id);
            List<Long> cs = children.getOrDefault(id, List.of());
            dq.addAll(cs);
        }
        return out;
    }
}
