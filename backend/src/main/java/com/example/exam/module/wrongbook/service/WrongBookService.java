package com.example.exam.module.wrongbook.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.module.wrongbook.dto.WrongBookVO;

public interface WrongBookService {

    void syncAfterExamSubmit(Long examRecordId);

    Page<WrongBookVO> pageForStudent(Long courseId, long page, long size);
}
