package com.example.exam.module.question.service;

import com.example.exam.module.question.dto.KnowledgePointCreateRequest;
import com.example.exam.module.question.dto.KnowledgePointUpdateRequest;
import com.example.exam.module.question.entity.KnowledgePoint;

import java.util.List;

public interface KnowledgePointService {

    KnowledgePoint create(KnowledgePointCreateRequest req);

    KnowledgePoint update(Long id, KnowledgePointUpdateRequest req);

    void delete(Long id);

    List<KnowledgePoint> listByCourse(Long courseId);

    /**
     * 包含自身及所有子孙知识点 ID（同课程内）。
     */
    List<Long> listSelfAndDescendantIds(Long knowledgePointId);
}
