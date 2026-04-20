package com.example.exam.module.paper.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.paper.dto.PaperAutoGenRequest;
import com.example.exam.module.paper.dto.PaperAutoGenResult;
import com.example.exam.module.paper.dto.PaperDetailVO;
import com.example.exam.module.paper.dto.PaperFromTemplateRequest;
import com.example.exam.module.paper.dto.PaperManualRequest;
import com.example.exam.module.paper.entity.Paper;
import com.example.exam.module.paper.entity.PaperGenerationLog;

public interface PaperService {

    Paper createManual(PaperManualRequest req);

    PaperAutoGenResult generateAuto(PaperAutoGenRequest req);

    PaperAutoGenResult generateFromTemplate(Long templateId, PaperFromTemplateRequest req);

    Page<Paper> page(Long courseId, long page, long size);

    Page<PaperGenerationLog> pageGenerationLogs(Long courseId, long page, long size);

    PaperDetailVO getDetail(Long id);

    void delete(Long id);
}
