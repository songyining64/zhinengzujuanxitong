package com.example.exam.websocket.service;

import com.example.exam.websocket.dto.ExamEventDTO;

/**
 * 考试WebSocket服务接口
 */
public interface ExamWebSocketService {

    /**
     * 发送考试开始通知
     * @param examId 考试ID
     * @param examTitle 考试标题
     * @param endTime 结束时间
     */
    void sendExamStarted(Long examId, String examTitle, java.time.LocalDateTime endTime);

    /**
     * 发送考试结束通知
     * @param examId 考试ID
     * @param examTitle 考试标题
     */
    void sendExamEnded(Long examId, String examTitle);

    /**
     * 发送考试提醒通知
     * @param examId 考试ID
     * @param examTitle 考试标题
     * @param warningMessage 提醒消息
     * @param remainingMinutes 剩余分钟数
     */
    void sendExamWarning(Long examId, String examTitle, String warningMessage, Integer remainingMinutes);

    /**
     * 发送剩余时间通知
     * @param examId 考试ID
     * @param remainingMinutes 剩余分钟数
     */
    void sendTimeRemaining(Long examId, Integer remainingMinutes);

    /**
     * 发送考试事件
     * @param examId 考试ID
     * @param event 考试事件
     */
    void sendExamEvent(Long examId, ExamEventDTO event);

    /**
     * 发送消息到特定用户
     * @param userId 用户ID
     * @param examId 考试ID
     * @param message 消息
     */
    void sendToUser(Long userId, Long examId, String message);

    /**
     * 发送消息到考试房间
     * @param examId 考试ID
     * @param message 消息
     */
    void sendToExamRoom(Long examId, String message);

    /**
     * 获取考试房间在线人数
     * @param examId 考试ID
     * @return 在线人数
     */
    int getOnlineCount(Long examId);

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @param examId 考试ID
     * @return 是否在线
     */
    boolean isUserOnline(Long userId, Long examId);
}