package com.example.exam.module.exam.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 待教师批改的简答题（一条记录对应一题） */
@Data
public class SubjectivePendingVO {

    private Long examRecordId;

    private Long examId;

    private String examTitle;

    private Long studentId;

    private String studentUsername;

    private String studentRealName;

    private Long questionId;

    private String stem;

    private String userAnswer;

    /** 本题满分（来自试卷） */
    private BigDecimal maxScore;
}
