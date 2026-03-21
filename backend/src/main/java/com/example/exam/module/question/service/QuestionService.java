package com.example.exam.module.question.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.question.dto.QuestionCreateRequest;
import com.example.exam.module.question.dto.QuestionSaveRequest;
import com.example.exam.module.question.dto.QuestionUpdateRequest;
import com.example.exam.module.question.entity.Question;

public interface QuestionService {

    Question create(QuestionCreateRequest req);

    Question update(Long id, QuestionUpdateRequest req);

    Question save(QuestionSaveRequest req);

    void delete(Long id);

    Question get(Long id);

    Page<Question> page(Long courseId, Long knowledgePointId, String type, String keyword, long page, long size);
}
