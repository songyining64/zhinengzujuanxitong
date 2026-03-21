package com.example.exam.module.question.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Excel 导入导出列（与模板一致）。
 */
@Data
public class QuestionExcelRow {

    @ExcelProperty("题型")
    private String type;

    @ExcelProperty("知识点ID")
    private Long knowledgePointId;

    @ExcelProperty("题干")
    private String stem;

    @ExcelProperty("选项JSON")
    private String optionsJson;

    @ExcelProperty("答案")
    private String answer;

    @ExcelProperty("解析")
    private String analysis;

    @ExcelProperty("难度")
    private Integer difficulty;

    @ExcelProperty("默认分值")
    private BigDecimal scoreDefault;

    @ExcelProperty("审核状态")
    private String reviewStatus;

    /** 导出时带主键，导入忽略 */
    @ExcelProperty("试题ID")
    private Long id;
}
