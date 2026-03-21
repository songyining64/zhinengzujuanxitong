package com.example.exam.module.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("question_version")
public class QuestionVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private Integer versionNo;

    private Long knowledgePointId;

    private String type;

    private String stem;

    private String optionsJson;

    private String answer;

    private String analysis;

    private BigDecimal scoreDefault;

    private Integer difficulty;

    private Integer status;

    private String reviewStatus;

    private Long editorId;

    private LocalDateTime createTime;
}
