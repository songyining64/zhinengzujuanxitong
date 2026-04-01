package com.example.exam.module.paper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("paper")
public class Paper {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String title;

    private String mode;

    private BigDecimal totalScore;

    private Long randomSeed;

    private String rulesJson;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
