import { ref, reactive, onUnmounted, onMounted } from 'vue';
import { ExamWebSocketClient, ExamWebSocketOptions, WebSocketMessage, ExamEvent } from '@/utils/websocket';

/**
 * WebSocket Composables
 * 用于在Vue组件中管理WebSocket连接
 */

export interface UseWebSocketReturn {
  // 状态
  isConnected: boolean;
  connectionState: string;
  notifications: Array<{
    title: string;
    message: string;
    type: 'info' | 'warning' | 'error';
    timestamp: Date;
  }>;
  examEvents: ExamEvent[];

  // 方法
  connect: () => void;
  disconnect: () => void;
  sendMessage: (message: any) => void;
  joinExam: () => void;
  leaveExam: () => void;
  clearNotifications: () => void;
  clearExamEvents: () => void;

  // 客户端实例
  client: ExamWebSocketClient | null;
}

/**
 * 使用WebSocket
 * @param options WebSocket配置选项
 */
export function useWebSocket(options: ExamWebSocketOptions): UseWebSocketReturn {
  const client = ref<ExamWebSocketClient | null>(null);
  const isConnected = ref(false);
  const connectionState = ref('DISCONNECTED');
  const notifications = reactive<Array<{
    title: string;
    message: string;
    type: 'info' | 'warning' | 'error';
    timestamp: Date;
  }>>([]);
  const examEvents = reactive<ExamEvent[]>([]);

  // 创建WebSocket客户端
  const createClient = () => {
    const wsOptions: ExamWebSocketOptions = {
      examId: options.examId,
      userId: options.userId,
      onMessage: (message: WebSocketMessage) => {
        console.log('收到WebSocket消息:', message);
        if (options.onMessage) {
          options.onMessage(message);
        }
      },
      onExamEvent: (event: ExamEvent) => {
        console.log('收到考试事件:', event);
        examEvents.unshift(event); // 添加到事件列表顶部

        // 显示通知
        showEventNotification(event);

        if (options.onExamEvent) {
          options.onExamEvent(event);
        }
      },
      onError: (error: any) => {
        console.error('WebSocket错误:', error);
        showNotification('连接错误', error.message || 'WebSocket连接错误', 'error');
        if (options.onError) {
          options.onError(error);
        }
      },
      onConnected: () => {
        console.log('WebSocket已连接');
        isConnected.value = true;
        connectionState.value = 'CONNECTED';
        showNotification('连接成功', '已连接到考试服务器', 'info');
        if (options.onConnected) {
          options.onConnected();
        }
      },
      onDisconnected: () => {
        console.log('WebSocket已断开');
        isConnected.value = false;
        connectionState.value = 'DISCONNECTED';
        showNotification('连接断开', '与考试服务器的连接已断开', 'warning');
        if (options.onDisconnected) {
          options.onDisconnected();
        }
      }
    };

    client.value = new ExamWebSocketClient(wsOptions);
  };

  // 显示通知
  const showNotification = (title: string, message: string, type: 'info' | 'warning' | 'error') => {
    const notification = {
      title,
      message,
      type,
      timestamp: new Date()
    };

    notifications.unshift(notification); // 添加到通知列表顶部

    // 限制通知数量
    if (notifications.length > 50) {
      notifications.splice(50);
    }

    // 触发自定义事件，供其他组件监听
    window.dispatchEvent(new CustomEvent('websocket-notification', {
      detail: notification
    }));
  };

  // 显示考试事件通知
  const showEventNotification = (event: ExamEvent) => {
    let title = '考试事件';
    let type: 'info' | 'warning' | 'error' = 'info';

    switch (event.eventType) {
      case 'EXAM_STARTED':
        title = '考试开始';
        type = 'info';
        break;
      case 'EXAM_ENDED':
        title = '考试结束';
        type = 'warning';
        break;
      case 'EXAM_WARNING':
        title = '考试提醒';
        type = 'info';
        break;
      case 'EXAM_FORCE_SUBMIT':
        title = '强制交卷';
        type = 'error';
        break;
      case 'EXAM_TIME_REMAINING':
        title = '剩余时间';
        type = 'info';
        break;
      default:
        title = '考试通知';
    }

    showNotification(title, event.description, type);
  };

  // 连接WebSocket
  const connect = () => {
    if (!client.value) {
      createClient();
    }

    if (client.value && !client.value.isConnected()) {
      client.value.connect();
      updateConnectionState();
    }
  };

  // 断开连接
  const disconnect = () => {
    if (client.value) {
      client.value.leaveExam();
      client.value.disconnect();
      client.value = null;
    }
    isConnected.value = false;
    connectionState.value = 'DISCONNECTED';
  };

  // 发送消息
  const sendMessage = (message: any) => {
    if (client.value && client.value.isConnected()) {
      client.value.send(message);
    } else {
      console.warn('无法发送消息，WebSocket未连接');
    }
  };

  // 加入考试房间
  const joinExam = () => {
    if (client.value && client.value.isConnected()) {
      client.value.joinExam();
    }
  };

  // 离开考试房间
  const leaveExam = () => {
    if (client.value) {
      client.value.leaveExam();
    }
  };

  // 更新连接状态
  const updateConnectionState = () => {
    if (client.value) {
      connectionState.value = client.value.getConnectionState();
    }
  };

  // 清空通知
  const clearNotifications = () => {
    notifications.splice(0, notifications.length);
  };

  // 清空考试事件
  const clearExamEvents = () => {
    examEvents.splice(0, examEvents.length);
  };

  // 监听浏览器通知权限
  const requestNotificationPermission = () => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  };

  // 组件挂载时请求通知权限
  onMounted(() => {
    requestNotificationPermission();
  });

  // 组件卸载时断开连接
  onUnmounted(() => {
    disconnect();
  });

  return {
    // 状态
    isConnected,
    connectionState,
    notifications,
    examEvents,

    // 方法
    connect,
    disconnect,
    sendMessage,
    joinExam,
    leaveExam,
    clearNotifications,
    clearExamEvents,

    // 客户端实例
    client: client.value
  };
}

/**
 * 创建考试WebSocket连接
 * 简化版本，适用于考试页面
 */
export function useExamWebSocket(examId: number, userId: number) {
  const options: ExamWebSocketOptions = {
    examId,
    userId,
    onExamEvent: (event) => {
      // 处理重要的考试事件
      if (event.eventType === 'EXAM_ENDED') {
        console.log('考试结束，准备自动交卷');
        // 触发自动交卷逻辑
        window.dispatchEvent(new CustomEvent('exam-auto-submit', {
          detail: { examId }
        }));
      } else if (event.eventType === 'EXAM_FORCE_SUBMIT') {
        console.log('强制交卷');
        window.dispatchEvent(new CustomEvent('exam-force-submit', {
          detail: { examId }
        }));
      }
    }
  };

  return useWebSocket(options);
}