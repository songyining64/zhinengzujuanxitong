package com.example.exam.module.paper.service;

import com.example.exam.module.paper.dto.PaperTemplateSaveRequest;
import com.example.exam.module.paper.entity.PaperTemplate;

import java.util.List;

public interface PaperTemplateService {

    PaperTemplate create(PaperTemplateSaveRequest req);

    PaperTemplate update(Long id, PaperTemplateSaveRequest req);

    void delete(Long id);

    List<PaperTemplate> listByCourse(Long courseId);

    PaperTemplate get(Long id);
}
