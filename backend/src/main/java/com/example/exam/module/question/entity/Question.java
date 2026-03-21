package com.example.exam.module.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long knowledgePointId;

    private String type;

    private String stem;

    private String optionsJson;

    private String answer;

    private String analysis;

    private BigDecimal scoreDefault;

    private Integer difficulty;

    private Long creatorId;

    private Integer status;

    /** 审核状态：见 {@link com.example.exam.common.enums.QuestionReviewStatusEnum} */
    private String reviewStatus;

    /** 当前版本号，每次归档后递增 */
    private Integer versionNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
