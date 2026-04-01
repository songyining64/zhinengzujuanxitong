package com.example.exam.websocket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试事件数据
 */
@Data
public class ExamEventDTO {

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 考试ID
     */
    private Long examId;

    /**
     * 考试标题
     */
    private String examTitle;

    /**
     * 事件描述
     */
    private String description;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 额外数据
     */
    private Object extraData;

    /**
     * 事件类型常量
     */
    public static class EventType {
        public static final String EXAM_STARTED = "EXAM_STARTED";
        public static final String EXAM_ENDED = "EXAM_ENDED";
        public static final String EXAM_WARNING = "EXAM_WARNING";
        public static final String EXAM_PAUSED = "EXAM_PAUSED";
        public static final String EXAM_RESUMED = "EXAM_RESUMED";
        public static final String EXAM_FORCE_SUBMIT = "EXAM_FORCE_SUBMIT";
        public static final String EXAM_TIME_REMAINING = "EXAM_TIME_REMAINING";
        public static final String EXAM_STUDENT_JOINED = "EXAM_STUDENT_JOINED";
        public static final String EXAM_STUDENT_LEFT = "EXAM_STUDENT_LEFT";
        public static final String EXAM_SUBMISSION_REMINDER = "EXAM_SUBMISSION_REMINDER";
    }

    /**
     * 创建考试开始事件
     */
    public static ExamEventDTO examStarted(Long examId, String examTitle, LocalDateTime endTime) {
        ExamEventDTO dto = new ExamEventDTO();
        dto.setEventType(EventType.EXAM_STARTED);
        dto.setExamId(examId);
        dto.setExamTitle(examTitle);
        dto.setDescription("考试已开始");
        dto.setEventTime(LocalDateTime.now());
        dto.setExtraData(new ExamStartedData(endTime));
        return dto;
    }

    /**
     * 创建考试结束事件
     */
    public static ExamEventDTO examEnded(Long examId, String examTitle) {
        ExamEventDTO dto = new ExamEventDTO();
        dto.setEventType(EventType.EXAM_ENDED);
        dto.setExamId(examId);
        dto.setExamTitle(examTitle);
        dto.setDescription("考试已结束");
        dto.setEventTime(LocalDateTime.now());
        return dto;
    }

    /**
     * 创建考试提醒事件
     */
    public static ExamEventDTO examWarning(Long examId, String examTitle, String warningMessage, Integer remainingMinutes) {
        ExamEventDTO dto = new ExamEventDTO();
        dto.setEventType(EventType.EXAM_WARNING);
        dto.setExamId(examId);
        dto.setExamTitle(examTitle);
        dto.setDescription(warningMessage);
        dto.setEventTime(LocalDateTime.now());
        dto.setExtraData(new ExamWarningData(remainingMinutes));
        return dto;
    }

    /**
     * 创建剩余时间事件
     */
    public static ExamEventDTO timeRemaining(Long examId, Integer remainingMinutes) {
        ExamEventDTO dto = new ExamEventDTO();
        dto.setEventType(EventType.EXAM_TIME_REMAINING);
        dto.setExamId(examId);
        dto.setDescription(String.format("距离考试结束还有 %d 分钟", remainingMinutes));
        dto.setEventTime(LocalDateTime.now());
        dto.setExtraData(new TimeRemainingData(remainingMinutes));
        return dto;
    }

    /**
     * 考试开始数据
     */
    @Data
    public static class ExamStartedData {
        private LocalDateTime endTime;

        public ExamStartedData(LocalDateTime endTime) {
            this.endTime = endTime;
        }
    }

    /**
     * 考试提醒数据
     */
    @Data
    public static class ExamWarningData {
        private Integer remainingMinutes;

        public ExamWarningData(Integer remainingMinutes) {
            this.remainingMinutes = remainingMinutes;
        }
    }

    /**
     * 剩余时间数据
     */
    @Data
    public static class TimeRemainingData {
        private Integer remainingMinutes;

        public TimeRemainingData(Integer remainingMinutes) {
            this.remainingMinutes = remainingMinutes;
        }
    }
}