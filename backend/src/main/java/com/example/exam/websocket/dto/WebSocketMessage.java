package com.example.exam.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket通用消息格式
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 考试ID
     */
    private Long examId;

    /**
     * 用户ID（发送者或目标用户）
     */
    private Long userId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 数据负载（JSON格式）
     */
    private Object data;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 创建系统消息
     */
    public static WebSocketMessage systemMessage(String content, Long examId) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("SYSTEM");
        message.setContent(content);
        message.setExamId(examId);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    /**
     * 创建考试事件消息
     */
    public static WebSocketMessage examEvent(String eventType, Long examId, Object data) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("EXAM_EVENT");
        message.setContent(eventType);
        message.setExamId(examId);
        message.setData(data);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    /**
     * 创建错误消息
     */
    public static WebSocketMessage error(String error, Long examId) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("ERROR");
        message.setContent(error);
        message.setExamId(examId);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
}