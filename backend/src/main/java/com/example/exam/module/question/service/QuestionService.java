package com.example.exam.module.question.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.question.dto.QuestionBatchUpdateRequest;
import com.example.exam.module.question.dto.QuestionCreateRequest;
import com.example.exam.module.question.dto.QuestionDedupCheckRequest;
import com.example.exam.module.question.dto.QuestionDedupResultVO;
import com.example.exam.module.question.dto.QuestionImportResultVO;
import com.example.exam.module.question.dto.QuestionSaveRequest;
import com.example.exam.module.question.dto.QuestionUpdateRequest;
import com.example.exam.module.question.dto.QuestionVersionVO;
import com.example.exam.module.question.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface QuestionService {

    Question create(QuestionCreateRequest req);

    Question update(Long id, QuestionUpdateRequest req);

    Question save(QuestionSaveRequest req);

    void delete(Long id);

    Question get(Long id);

    Page<Question> page(Long courseId, Long knowledgePointId, String type, String keyword,
                        String reviewStatus, long page, long size);

    int batchUpdate(QuestionBatchUpdateRequest req);

    QuestionImportResultVO importExcel(Long courseId, MultipartFile file);

    void exportExcel(Long courseId, HttpServletResponse response) throws IOException;

    void downloadImportTemplate(HttpServletResponse response) throws IOException;

    void submitReview(Long id);

    void approveReview(Long id);

    void rejectReview(Long id);

    /** 查重：题干 + 选项 JSON 综合相似度 */
    QuestionDedupResultVO dedupCheck(QuestionDedupCheckRequest req);

    /** 历史版本列表（不含当前最新内容，当前见 question 表） */
    List<QuestionVersionVO> listVersions(Long questionId);

    /** 指定历史版本快照 */
    QuestionVersionVO getVersionSnapshot(Long questionId, int versionNo);
}
