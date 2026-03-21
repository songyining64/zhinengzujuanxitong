package com.example.exam.module.paper.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaperTemplateSaveRequest {

    @NotNull
    private Long courseId;

    @NotBlank
    private String name;

    /** 自动组卷规则（不含试卷标题，保存时会写入 rules_json） */
    @NotNull
    @Valid
    private PaperAutoGenRequest rules;
}
