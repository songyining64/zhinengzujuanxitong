package com.example.exam.module.exam.service;

import com.example.exam.module.exam.dto.SubjectiveGradeRequest;
import com.example.exam.module.exam.dto.SubjectivePendingVO;

import java.util.List;

public interface ExamGradingService {

    void gradeSubjective(SubjectiveGradeRequest req);

    /** 已交卷但简答题尚未打分的条目（按考试筛选） */
    List<SubjectivePendingVO> listPendingSubjective(Long examId);
}
