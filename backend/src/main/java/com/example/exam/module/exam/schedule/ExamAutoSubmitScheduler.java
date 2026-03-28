package com.example.exam.module.exam.schedule;

import com.example.exam.module.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExamAutoSubmitScheduler {

    private final ExamService examService;

    @Scheduled(fixedDelayString = "${exam.auto-submit-interval-ms:60000}")
    public void autoSubmit() {
        try {
            int n = examService.autoSubmitExpiredRecords();
            if (n > 0) {
                log.info("自动交卷处理 {} 条记录", n);
            }
        } catch (Exception e) {
            log.warn("自动交卷任务异常", e);
        }
    }
}
