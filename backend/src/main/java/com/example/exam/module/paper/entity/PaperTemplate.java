package com.example.exam.module.paper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paper_template")
public class PaperTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String name;

    /** 与自动组卷规则一致（JSON），不含 title */
    private String rulesJson;

    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
