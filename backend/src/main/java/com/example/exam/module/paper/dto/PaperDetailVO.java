package com.example.exam.module.paper.dto;

import com.example.exam.module.paper.entity.Paper;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperDetailVO {

    private Paper paper;

    private List<PaperQuestionLine> questions;

    @Data
    public static class PaperQuestionLine {
        private Long paperQuestionId;
        private Long questionId;
        private Integer questionOrder;
        private BigDecimal score;
        private String type;
        private String stem;
    }
}
