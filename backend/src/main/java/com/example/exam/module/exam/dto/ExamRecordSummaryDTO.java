package com.example.exam.module.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单条考试记录摘要（学生本人或教师查看），减少前端多次请求。
 */
@Data
public class ExamRecordSummaryDTO {

    private Long recordId;

    private Long examId;

    private String examTitle;

    private Long courseId;

    /** IN_PROGRESS / SUBMITTED / EXPIRED */
    private String recordStatus;

    private BigDecimal totalScore;

    private BigDecimal passScore;

    /** 相对及格线是否及格，未设及格线时为 null */
    private Boolean passed;

    /** 名次，未交卷或学生端成绩未发布时为 null */
    private Integer rank;

    /** 参与排名的已交卷人数 */
    private Integer rankTotal;

    private Boolean scorePublished;

    private Integer switchBlurCount;

    private Integer switchBlurLimit;

    private LocalDateTime submittedAt;

    /** 本场开始时间（进行中/已交卷） */
    private LocalDateTime startedAt;

    /** 本场截止交卷时间（与开考接口计算规则一致） */
    private LocalDateTime deadlineAt;
}
