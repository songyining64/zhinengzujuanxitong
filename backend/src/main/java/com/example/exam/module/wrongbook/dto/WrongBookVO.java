package com.example.exam.module.wrongbook.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WrongBookVO {

    private Long id;

    private Long courseId;

    private Long questionId;

    private String stem;

    private String type;

    private Integer wrongCount;

    private LocalDateTime lastWrongAt;
}
