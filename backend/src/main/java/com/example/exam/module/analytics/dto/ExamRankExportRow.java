package com.example.exam.module.analytics.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExamRankExportRow {

    @ExcelProperty("排名")
    private Integer rank;

    @ExcelProperty("学号/用户名")
    private String username;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("总分")
    private BigDecimal totalScore;
}
