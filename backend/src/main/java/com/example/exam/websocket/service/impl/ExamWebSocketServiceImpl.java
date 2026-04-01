package com.example.exam.websocket.service.impl;

import com.example.exam.websocket.dto.ExamEventDTO;
import com.example.exam.websocket.dto.WebSocketMessage;
import com.example.exam.websocket.service.ExamWebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 考试WebSocket服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamWebSocketServiceImpl implements ExamWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 考试房间在线用户存储
     * key: examId, value: Set of userIds
     */
    private final ConcurrentMap<Long, CopyOnWriteArraySet<Long>> examRooms = new ConcurrentHashMap<>();

    /**
     * 用户会话存储
     * key: "userId-examId", value: sessionId
     */
    private final ConcurrentMap<String, String> userSessions = new ConcurrentHashMap<>();

    @Override
    public void sendExamStarted(Long examId, String examTitle, LocalDateTime endTime) {
        ExamEventDTO event = ExamEventDTO.examStarted(examId, examTitle, endTime);
        sendExamEvent(examId, event);
        log.info("考试开始通知已发送，考试ID: {}, 标题: {}", examId, examTitle);
    }

    @Override
    public void sendExamEnded(Long examId, String examTitle) {
        ExamEventDTO event = ExamEventDTO.examEnded(examId, examTitle);
        sendExamEvent(examId, event);
        log.info("考试结束通知已发送，考试ID: {}, 标题: {}", examId, examTitle);
    }

    @Override
    public void sendExamWarning(Long examId, String examTitle, String warningMessage, Integer remainingMinutes) {
        ExamEventDTO event = ExamEventDTO.examWarning(examId, examTitle, warningMessage, remainingMinutes);
        sendExamEvent(examId, event);
        log.info("考试提醒通知已发送，考试ID: {}, 消息: {}", examId, warningMessage);
    }

    @Override
    public void sendTimeRemaining(Long examId, Integer remainingMinutes) {
        ExamEventDTO event = ExamEventDTO.timeRemaining(examId, remainingMinutes);
        sendExamEvent(examId, event);
    }

    @Override
    public void sendExamEvent(Long examId, ExamEventDTO event) {
        try {
            WebSocketMessage message = WebSocketMessage.examEvent(event.getEventType(), examId, event);
            String destination = String.format("/topic/exam/%d/events", examId);
            messagingTemplate.convertAndSend(destination, message);
            log.debug("考试事件已发送，目的地: {}, 事件类型: {}", destination, event.getEventType());
        } catch (Exception e) {
            log.error("发送考试事件失败，考试ID: {}, 事件: {}", examId, event, e);
        }
    }

    @Override
    public void sendToUser(Long userId, Long examId, String message) {
        try {
            WebSocketMessage wsMessage = WebSocketMessage.systemMessage(message, examId);
            String destination = String.format("/user/%d/exam/%d/messages", userId, examId);
            messagingTemplate.convertAndSendToUser(userId.toString(), destination, wsMessage);
            log.debug("用户消息已发送，用户ID: {}, 考试ID: {}, 消息: {}", userId, examId, message);
        } catch (Exception e) {
            log.error("发送用户消息失败，用户ID: {}, 考试ID: {}", userId, examId, e);
        }
    }

    @Override
    public void sendToExamRoom(Long examId, String message) {
        try {
            WebSocketMessage wsMessage = WebSocketMessage.systemMessage(message, examId);
            String destination = String.format("/topic/exam/%d/messages", examId);
            messagingTemplate.convertAndSend(destination, wsMessage);
            log.debug("考试房间消息已发送，考试ID: {}, 消息: {}", examId, message);
        } catch (Exception e) {
            log.error("发送考试房间消息失败，考试ID: {}", examId, e);
        }
    }

    @Override
    public int getOnlineCount(Long examId) {
        CopyOnWriteArraySet<Long> users = examRooms.get(examId);
        return users != null ? users.size() : 0;
    }

    @Override
    public boolean isUserOnline(Long userId, Long examId) {
        String key = getSessionKey(userId, examId);
        return userSessions.containsKey(key);
    }

    /**
     * 用户加入考试房间
     * @param userId 用户ID
     * @param examId 考试ID
     * @param sessionId 会话ID
     */
    public void userJoinExam(Long userId, Long examId, String sessionId) {
        // 记录用户会话
        String sessionKey = getSessionKey(userId, examId);
        userSessions.put(sessionKey, sessionId);

        // 添加到考试房间
        examRooms.computeIfAbsent(examId, k -> new CopyOnWriteArraySet<>()).add(userId);

        // 发送用户加入通知
        ExamEventDTO event = new ExamEventDTO();
        event.setEventType(ExamEventDTO.EventType.EXAM_STUDENT_JOINED);
        event.setExamId(examId);
        event.setDescription("学生已加入考试");
        event.setEventTime(LocalDateTime.now());
        event.setExtraData(Map.of("userId", userId, "onlineCount", getOnlineCount(examId)));

        sendExamEvent(examId, event);
        log.info("用户加入考试房间，用户ID: {}, 考试ID: {}, 当前在线: {}", userId, examId, getOnlineCount(examId));
    }

    /**
     * 用户离开考试房间
     * @param userId 用户ID
     * @param examId 考试ID
     */
    public void userLeaveExam(Long userId, Long examId) {
        // 移除用户会话
        String sessionKey = getSessionKey(userId, examId);
        userSessions.remove(sessionKey);

        // 从考试房间移除
        CopyOnWriteArraySet<Long> users = examRooms.get(examId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                examRooms.remove(examId);
            }
        }

        // 发送用户离开通知
        ExamEventDTO event = new ExamEventDTO();
        event.setEventType(ExamEventDTO.EventType.EXAM_STUDENT_LEFT);
        event.setExamId(examId);
        event.setDescription("学生已离开考试");
        event.setEventTime(LocalDateTime.now());
        event.setExtraData(Map.of("userId", userId, "onlineCount", getOnlineCount(examId)));

        sendExamEvent(examId, event);
        log.info("用户离开考试房间，用户ID: {}, 考试ID: {}, 剩余在线: {}", userId, examId, getOnlineCount(examId));
    }

    /**
     * 获取会话键
     */
    private String getSessionKey(Long userId, Long examId) {
        return userId + "-" + examId;
    }

    /**
     * 清理无效会话
     */
    public void cleanupSession(String sessionId) {
        // 查找并清理该会话对应的用户
        userSessions.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(sessionId)) {
                // 解析用户ID和考试ID
                String[] parts = entry.getKey().split("-");
                if (parts.length == 2) {
                    try {
                        Long userId = Long.parseLong(parts[0]);
                        Long examId = Long.parseLong(parts[1]);
                        userLeaveExam(userId, examId);
                    } catch (NumberFormatException e) {
                        log.error("解析会话键失败: {}", entry.getKey(), e);
                    }
                }
                return true;
            }
            return false;
        });
    }
}