package com.example.exam.module.exam.service;

import com.example.exam.module.exam.dto.SubjectiveGradeRequest;

public interface ExamGradingService {

    void gradeSubjective(SubjectiveGradeRequest req);
}
