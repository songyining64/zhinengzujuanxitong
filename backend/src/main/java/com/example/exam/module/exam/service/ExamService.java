package com.example.exam.module.exam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.exam.dto.ExamCreateRequest;
import com.example.exam.module.exam.dto.ExamStartResponse;
import com.example.exam.module.exam.dto.ExamStudentVO;
import com.example.exam.module.exam.dto.SaveAnswersRequest;
import com.example.exam.module.exam.dto.SubmitAnswerRequest;
import com.example.exam.module.exam.dto.TakeQuestionVO;
import com.example.exam.module.exam.entity.Exam;

import java.util.List;

public interface ExamService {

    Exam create(ExamCreateRequest req);

    Exam update(Long id, ExamCreateRequest req);

    void publish(Long id);

    void endExam(Long id);

    Page<Exam> pageForTeacher(Long courseId, long page, long size);

    Page<ExamStudentVO> pageForStudent(long page, long size, String keyword);

    Exam get(Long id);

    ExamStartResponse startExam(Long examId);

    void ensureExamAnswerRows(Long examRecordId);

    List<TakeQuestionVO> listTakeQuestions(Long examRecordId);

    void saveAnswers(Long examRecordId, SaveAnswersRequest req);

    void submitAnswers(Long examRecordId, SubmitAnswerRequest req);

    int autoSubmitExpiredRecords();

    void gradeAndFinalizeRecord(Long examRecordId);
}
