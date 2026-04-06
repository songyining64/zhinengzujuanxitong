package com.example.exam.module.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExamCreateRequest {

    @NotNull
    private Long courseId;

    @NotNull
    private Long paperId;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @NotNull
    private Integer durationMinutes;

    /** 及格线，null 表示不设 */
    private BigDecimal passScore;

    /** 成绩是否已发布（学生可见排名与统计），默认 false */
    private Boolean scorePublished;

    /** 题目乱序，默认 true */
    private Boolean shuffleQuestions;

    /** 客观题选项乱序，默认 true */
    private Boolean shuffleOptions;

    /** 切屏次数上限，null 或 0 表示不限制 */
    private Integer switchBlurLimit;
}
