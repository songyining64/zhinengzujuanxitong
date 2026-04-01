package com.example.exam.module.analytics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.analytics.dto.ExamKnowledgeStatDTO;
import com.example.exam.module.analytics.dto.ExamOverviewDTO;
import com.example.exam.module.analytics.dto.ExamQuestionStatDTO;
import com.example.exam.module.analytics.dto.StudentRankDTO;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public interface ExamAnalyticsService {

    ExamOverviewDTO overview(Long examId);

    Page<StudentRankDTO> rank(Long examId, long page, long size);

    List<ExamQuestionStatDTO> questionStats(Long examId);

    List<ExamKnowledgeStatDTO> knowledgePointStats(Long examId);

    void exportRank(Long examId, HttpServletResponse response) throws IOException;
}
