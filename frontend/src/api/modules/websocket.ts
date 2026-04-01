import request from '../http';

/**
 * WebSocket管理API
 */

export interface WebSocketAdminRequest {
  examId?: number;
  examTitle?: string;
  endTime?: string;
  warningMessage?: string;
  remainingMinutes?: number;
}

/**
 * 触发考试开始通知
 */
export function triggerExamStart(examId: number, data: WebSocketAdminRequest) {
  return request.post(`/api/websocket/admin/exam/${examId}/start`, data);
}

/**
 * 触发考试结束通知
 */
export function triggerExamEnd(examId: number, data: WebSocketAdminRequest) {
  return request.post(`/api/websocket/admin/exam/${examId}/end`, data);
}

/**
 * 发送考试提醒通知
 */
export function sendExamWarning(examId: number, data: WebSocketAdminRequest) {
  return request.post(`/api/websocket/admin/exam/${examId}/warning`, data);
}

/**
 * 发送剩余时间通知
 */
export function sendTimeRemaining(examId: number, remainingMinutes: number) {
  return request.post(`/api/websocket/admin/exam/${examId}/time-remaining`, null, {
    params: { remainingMinutes }
  });
}

/**
 * 获取考试房间在线人数
 */
export function getOnlineCount(examId: number) {
  return request.get<number>(`/api/websocket/admin/exam/${examId}/online-count`);
}

/**
 * 检查用户是否在线
 */
export function isUserOnline(examId: number, userId: number) {
  return request.get<boolean>(`/api/websocket/admin/exam/${examId}/user/${userId}/online`);
}

/**
 * 广播消息到考试房间
 */
export function broadcastToExamRoom(examId: number, message: string) {
  return request.post(`/api/websocket/admin/exam/${examId}/broadcast`, null, {
    params: { message }
  });
}

/**
 * 发送消息到特定用户
 */
export function sendToUser(examId: number, userId: number, message: string) {
  return request.post(`/api/websocket/admin/exam/${examId}/user/${userId}/message`, null, {
    params: { message }
  });
}

/**
 * 获取考试列表（示例）
 */
export function getExamList() {
  return request.get('/api/exam/teacher', {
    params: {
      courseId: 1,
      page: 1,
      size: 100
    }
  });
}