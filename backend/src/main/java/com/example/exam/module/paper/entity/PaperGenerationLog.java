package com.example.exam.module.paper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paper_generation_log")
public class PaperGenerationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private Long courseId;

    private Long operatorId;

    /** AUTO / TEMPLATE */
    private String mode;

    private String rulesJson;

    private Integer durationMs;

    private LocalDateTime createTime;
}
