package com.example.exam.websocket.controller;

import com.example.exam.common.api.ApiResponse;
import com.example.exam.websocket.dto.WebSocketAdminRequest;
import com.example.exam.websocket.service.ExamWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * WebSocket管理员控制器（REST API）
 */
@RestController
@RequestMapping("/api/websocket/admin")
@RequiredArgsConstructor
@Slf4j
public class ExamWebSocketAdminController {

    private final ExamWebSocketService examWebSocketService;

    /**
     * 管理员触发考试开始通知
     */
    @PostMapping("/exam/{examId}/start")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Void> triggerExamStart(
            @PathVariable Long examId,
            @RequestBody WebSocketAdminRequest request) {

        log.info("管理员触发考试开始通知，考试ID: {}, 请求: {}", examId, request);

        // 发送考试开始通知
        examWebSocketService.sendExamStarted(
                examId,
                request.getExamTitle(),
                request.getEndTime()
        );

        return ApiResponse.success();
    }

    /**
     * 管理员触发考试结束通知
     */
    @PostMapping("/exam/{examId}/end")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Void> triggerExamEnd(
            @PathVariable Long examId,
            @RequestBody WebSocketAdminRequest request) {

        log.info("管理员触发考试结束通知，考试ID: {}, 请求: {}", examId, request);

        // 发送考试结束通知
        examWebSocketService.sendExamEnded(
                examId,
                request.getExamTitle()
        );

        return ApiResponse.success();
    }

    /**
     * 管理员发送考试提醒通知
     */
    @PostMapping("/exam/{examId}/warning")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Void> sendExamWarning(
            @PathVariable Long examId,
            @RequestBody WebSocketAdminRequest request) {

        log.info("管理员发送考试提醒通知，考试ID: {}, 请求: {}", examId, request);

        // 发送考试提醒通知
        examWebSocketService.sendExamWarning(
                examId,
                request.getExamTitle(),
                request.getWarningMessage(),
                request.getRemainingMinutes()
        );

        return ApiResponse.success();
    }

    /**
     * 发送剩余时间通知
     */
    @PostMapping("/exam/{examId}/time-remaining")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Void> sendTimeRemaining(
            @PathVariable Long examId,
            @RequestParam Integer remainingMinutes) {

        log.info("管理员发送剩余时间通知，考试ID: {}, 剩余分钟: {}", examId, remainingMinutes);

        // 发送剩余时间通知
        examWebSocketService.sendTimeRemaining(examId, remainingMinutes);

        return ApiResponse.success();
    }

    /**
     * 获取考试房间在线人数
     */
    @GetMapping("/exam/{examId}/online-count")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Integer> getOnlineCount(@PathVariable Long examId) {
        int count = examWebSocketService.getOnlineCount(examId);
        log.info("获取考试房间在线人数，考试ID: {}, 在线人数: {}", examId, count);
        return ApiResponse.success(count);
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/exam/{examId}/user/{userId}/online")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Boolean> isUserOnline(
            @PathVariable Long examId,
            @PathVariable Long userId) {

        boolean isOnline = examWebSocketService.isUserOnline(userId, examId);
        log.info("检查用户是否在线，考试ID: {}, 用户ID: {}, 在线状态: {}", examId, userId, isOnline);
        return ApiResponse.success(isOnline);
    }

    /**
     * 发送消息到考试房间
     */
    @PostMapping("/exam/{examId}/broadcast")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Void> broadcastToExamRoom(
            @PathVariable Long examId,
            @RequestParam String message) {

        log.info("管理员广播消息到考试房间，考试ID: {}, 消息: {}", examId, message);

        // 发送消息到考试房间
        examWebSocketService.sendToExamRoom(examId, message);

        return ApiResponse.success();
    }

    /**
     * 发送消息到特定用户
     */
    @PostMapping("/exam/{examId}/user/{userId}/message")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('exam:manage')")
    public ApiResponse<Void> sendToUser(
            @PathVariable Long examId,
            @PathVariable Long userId,
            @RequestParam String message) {

        log.info("管理员发送消息到特定用户，考试ID: {}, 用户ID: {}, 消息: {}", examId, userId, message);

        // 发送消息到特定用户
        examWebSocketService.sendToUser(userId, examId, message);

        return ApiResponse.success();
    }
}