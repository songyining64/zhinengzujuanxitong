package com.example.exam.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam")
public class Exam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long paperId;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer durationMinutes;

    /** 及格线，null 表示不设 */
    private BigDecimal passScore;

    /** 成绩是否已发布（学生可见排名与统计） */
    private Integer scorePublished;

    /** 题目乱序 */
    private Integer shuffleQuestions;

    /** 客观题选项乱序 */
    private Integer shuffleOptions;

    /** 切屏次数上限，null 或 0 表示不限制 */
    private Integer switchBlurLimit;

    private String status;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
