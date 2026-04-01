package com.example.exam.module.paper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaperFromTemplateRequest {

    /** 生成试卷标题 */
    @NotBlank(message = "请填写试卷标题")
    private String title;

    private Long randomSeed;
}
