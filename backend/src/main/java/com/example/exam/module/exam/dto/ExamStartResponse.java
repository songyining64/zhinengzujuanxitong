package com.example.exam.module.exam.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamStartResponse {

    private Long recordId;

    private Long examId;

    private LocalDateTime startedAt;

    private Integer durationMinutes;

    private LocalDateTime deadlineAt;

    /** 是否启用题目乱序 */
    private Boolean shuffleQuestions;

    /** 是否启用选项乱序 */
    private Boolean shuffleOptions;

    /** 切屏次数上限，null 表示不限制 */
    private Integer switchBlurLimit;
}
