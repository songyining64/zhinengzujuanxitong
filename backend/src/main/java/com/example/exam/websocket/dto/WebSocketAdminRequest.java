package com.example.exam.websocket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket管理员请求
 */
@Data
public class WebSocketAdminRequest {

    /**
     * 考试ID
     */
    private Long examId;

    /**
     * 考试标题
     */
    private String examTitle;

    /**
     * 结束时间（仅用于开始事件）
     */
    private LocalDateTime endTime;

    /**
     * 提醒消息（仅用于提醒事件）
     */
    private String warningMessage;

    /**
     * 剩余分钟数（仅用于提醒事件）
     */
    private Integer remainingMinutes;
}