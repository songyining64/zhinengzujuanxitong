package com.example.exam.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_answer")
public class ExamAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examRecordId;

    private Long questionId;

    private String userAnswer;

    private BigDecimal score;

    private Integer isCorrect;

    private LocalDateTime gradedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
