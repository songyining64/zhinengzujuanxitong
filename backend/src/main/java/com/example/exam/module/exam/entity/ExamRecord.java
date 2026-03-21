package com.example.exam.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long studentId;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    private BigDecimal totalScore;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
