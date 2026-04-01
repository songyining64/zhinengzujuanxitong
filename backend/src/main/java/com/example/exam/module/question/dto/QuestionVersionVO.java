package com.example.exam.module.question.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuestionVersionVO {

    private Long id;

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
