package com.example.exam.websocket.controller;

import com.example.exam.common.api.ApiResponse;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import com.example.exam.websocket.dto.WebSocketMessage;
import com.example.exam.websocket.service.ExamWebSocketService;
import com.example.exam.websocket.service.impl.ExamWebSocketServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket控制器
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ExamWebSocketController {

    private final ExamWebSocketService examWebSocketService;
    private final ExamWebSocketServiceImpl examWebSocketServiceImpl;
    private final UserService userService;

    /**
     * 用户订阅考试事件
     */
    @SubscribeMapping("/exam/{examId}/events")
    public WebSocketMessage subscribeToExamEvents(@DestinationVariable Long examId, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getCurrentUserId();
        String sessionId = headerAccessor.getSessionId();

        // 用户加入考试房间
        examWebSocketServiceImpl.userJoinExam(userId, examId, sessionId);

        log.info("用户订阅考试事件，用户ID: {}, 考试ID: {}, 会话ID: {}", userId, examId, sessionId);

        return WebSocketMessage.systemMessage("已连接到考试房间，开始接收实时通知", examId);
    }

    /**
     * 用户发送消息到考试房间
     */
    @MessageMapping("/exam/{examId}/send")
    @SendTo("/topic/exam/{examId}/messages")
    public WebSocketMessage sendToExamRoom(
            @DestinationVariable Long examId,
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = getCurrentUserId();
        message.setUserId(userId);
        message.setTimestamp(java.time.LocalDateTime.now());

        log.debug("用户发送消息到考试房间，用户ID: {}, 考试ID: {}, 消息: {}", userId, examId, message.getContent());

        return message;
    }

    /**
     * 用户发送私聊消息
     */
    @MessageMapping("/exam/{examId}/private/{targetUserId}")
    public void sendPrivateMessage(
            @DestinationVariable Long examId,
            @DestinationVariable Long targetUserId,
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = getCurrentUserId();
        message.setUserId(userId);
        message.setTimestamp(java.time.LocalDateTime.now());

        // 发送私聊消息
        examWebSocketService.sendToUser(targetUserId, examId, message.getContent());

        log.debug("用户发送私聊消息，发送者: {}, 接收者: {}, 考试ID: {}, 消息: {}",
                userId, targetUserId, examId, message.getContent());
    }

    /**
     * 用户离开考试房间
     */
    @MessageMapping("/exam/{examId}/leave")
    public void leaveExamRoom(
            @DestinationVariable Long examId,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = getCurrentUserId();
        String sessionId = headerAccessor.getSessionId();

        // 用户离开考试房间
        examWebSocketServiceImpl.userLeaveExam(userId, examId);

        log.info("用户离开考试房间，用户ID: {}, 考试ID: {}, 会话ID: {}", userId, examId, sessionId);
    }

    /**
     * 处理WebSocket连接断开
     */
    @MessageMapping("/exam/disconnect")
    public void handleDisconnect(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        // 清理会话
        examWebSocketServiceImpl.cleanupSession(sessionId);

        log.info("WebSocket连接断开，会话ID: {}", sessionId);
    }

    /**
     * 管理员触发考试开始事件
     */
    @MessageMapping("/exam/{examId}/admin/start")
    public void adminStartExam(
            @DestinationVariable Long examId,
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = getCurrentUserId();

        // 验证管理员权限
        if (!isAdmin(userId)) {
            log.warn("非管理员用户尝试触发考试开始，用户ID: {}, 考试ID: {}", userId, examId);
            return;
        }

        String examTitle = (String) payload.get("examTitle");
        java.time.LocalDateTime endTime = parseDateTime(payload.get("endTime"));

        // 发送考试开始通知
        examWebSocketService.sendExamStarted(examId, examTitle, endTime);

        log.info("管理员触发考试开始，管理员ID: {}, 考试ID: {}, 标题: {}", userId, examId, examTitle);
    }

    /**
     * 管理员触发考试结束事件
     */
    @MessageMapping("/exam/{examId}/admin/end")
    public void adminEndExam(
            @DestinationVariable Long examId,
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        Long userId = getCurrentUserId();

        // 验证管理员权限
        if (!isAdmin(userId)) {
            log.warn("非管理员用户尝试触发考试结束，用户ID: {}, 考试ID: {}", userId, examId);
            return;
        }

        String examTitle = (String) payload.get("examTitle");

        // 发送考试结束通知
        examWebSocketService.sendExamEnded(examId, examTitle);

        log.info("管理员触发考试结束，管理员ID: {}, 考试ID: {}, 标题: {}", userId, examId, examTitle);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            String username = SecurityHelper.requireUsername();
            User user = userService.findByUsername(username);
            if (user == null) {
                log.error("未找到用户: {}", username);
                throw new RuntimeException("用户不存在");
            }
            return user.getId();
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            throw new RuntimeException("获取用户ID失败");
        }
    }

    /**
     * 检查是否为管理员
     */
    private boolean isAdmin(Long userId) {
        // 这里需要根据实际的权限系统进行检查
        // 暂时返回true，实际项目中需要实现
        return true;
    }

    /**
     * 解析日期时间
     */
    private java.time.LocalDateTime parseDateTime(Object dateTimeObj) {
        if (dateTimeObj instanceof String) {
            return java.time.LocalDateTime.parse((String) dateTimeObj);
        } else if (dateTimeObj instanceof java.time.LocalDateTime) {
            return (java.time.LocalDateTime) dateTimeObj;
        }
        return java.time.LocalDateTime.now().plusHours(2); // 默认2小时后
    }
}