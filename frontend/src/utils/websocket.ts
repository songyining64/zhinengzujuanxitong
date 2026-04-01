/**
 * WebSocket工具类
 * 用于连接考试WebSocket服务器，接收实时通知
 */

export interface WebSocketMessage {
  type: string;
  examId?: number;
  userId?: number;
  content: string;
  data?: any;
  timestamp: string;
}

export interface ExamEvent {
  eventType: string;
  examId: number;
  examTitle?: string;
  description: string;
  eventTime: string;
  extraData?: any;
}

export interface ExamWebSocketOptions {
  examId: number;
  userId: number;
  onMessage?: (message: WebSocketMessage) => void;
  onExamEvent?: (event: ExamEvent) => void;
  onError?: (error: any) => void;
  onConnected?: () => void;
  onDisconnected?: () => void;
}

/**
 * 考试WebSocket客户端
 */
export class ExamWebSocketClient {
  private socket: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 3000; // 3秒
  private isConnecting = false;
  private options: ExamWebSocketOptions;

  // STOMP客户端（如果需要）
  private stompClient: any = null;

  // 事件类型常量
  static EventType = {
    EXAM_STARTED: 'EXAM_STARTED',
    EXAM_ENDED: 'EXAM_ENDED',
    EXAM_WARNING: 'EXAM_WARNING',
    EXAM_PAUSED: 'EXAM_PAUSED',
    EXAM_RESUMED: 'EXAM_RESUMED',
    EXAM_FORCE_SUBMIT: 'EXAM_FORCE_SUBMIT',
    EXAM_TIME_REMAINING: 'EXAM_TIME_REMAINING',
    EXAM_STUDENT_JOINED: 'EXAM_STUDENT_JOINED',
    EXAM_STUDENT_LEFT: 'EXAM_STUDENT_LEFT',
  };

  constructor(options: ExamWebSocketOptions) {
    this.options = options;
  }

  /**
   * 连接到WebSocket服务器
   */
  public connect(): void {
    if (this.isConnecting || this.socket?.readyState === WebSocket.OPEN) {
      return;
    }

    this.isConnecting = true;

    try {
      // 构建WebSocket URL
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.host;
      const wsUrl = `${protocol}//${host}/ws-exam`;

      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = this.handleOpen.bind(this);
      this.socket.onmessage = this.handleMessage.bind(this);
      this.socket.onerror = this.handleError.bind(this);
      this.socket.onclose = this.handleClose.bind(this);

      console.log('WebSocket连接中...', wsUrl);
    } catch (error) {
      console.error('创建WebSocket连接失败:', error);
      this.handleError(error);
      this.isConnecting = false;
    }
  }

  /**
   * 断开连接
   */
  public disconnect(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.isConnecting = false;
    this.reconnectAttempts = 0;
  }

  /**
   * 发送消息
   */
  public send(message: any): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      const messageStr = typeof message === 'string' ? message : JSON.stringify(message);
      this.socket.send(messageStr);
    } else {
      console.warn('WebSocket未连接，无法发送消息');
    }
  }

  /**
   * 发送加入考试房间请求
   */
  public joinExam(): void {
    const joinMessage = {
      type: 'JOIN_EXAM',
      examId: this.options.examId,
      userId: this.options.userId,
      timestamp: new Date().toISOString()
    };
    this.send(joinMessage);
  }

  /**
   * 发送离开考试房间请求
   */
  public leaveExam(): void {
    const leaveMessage = {
      type: 'LEAVE_EXAM',
      examId: this.options.examId,
      userId: this.options.userId,
      timestamp: new Date().toISOString()
    };
    this.send(leaveMessage);
    this.disconnect();
  }

  /**
   * 处理连接打开
   */
  private handleOpen(event: Event): void {
    console.log('WebSocket连接已建立');
    this.isConnecting = false;
    this.reconnectAttempts = 0;

    // 连接成功后加入考试房间
    this.joinExam();

    // 触发连接回调
    if (this.options.onConnected) {
      this.options.onConnected();
    }
  }

  /**
   * 处理收到消息
   */
  private handleMessage(event: MessageEvent): void {
    try {
      const message: WebSocketMessage = JSON.parse(event.data);
      console.log('收到WebSocket消息:', message);

      // 处理不同类型的消息
      switch (message.type) {
        case 'SYSTEM':
          if (this.options.onMessage) {
            this.options.onMessage(message);
          }
          break;

        case 'EXAM_EVENT':
          // 解析考试事件
          const examEvent: ExamEvent = message.data;
          if (this.options.onExamEvent) {
            this.options.onExamEvent(examEvent);
          }
          this.handleExamEvent(examEvent);
          break;

        case 'ERROR':
          console.error('WebSocket错误消息:', message.content);
          if (this.options.onError) {
            this.options.onError(new Error(message.content));
          }
          break;

        default:
          console.warn('未知类型的WebSocket消息:', message);
      }
    } catch (error) {
      console.error('解析WebSocket消息失败:', error, event.data);
    }
  }

  /**
   * 处理考试事件
   */
  private handleExamEvent(event: ExamEvent): void {
    console.log('处理考试事件:', event);

    // 根据事件类型执行不同操作
    switch (event.eventType) {
      case ExamWebSocketClient.EventType.EXAM_STARTED:
        this.showNotification('考试开始', event.description, 'info');
        break;

      case ExamWebSocketClient.EventType.EXAM_ENDED:
        this.showNotification('考试结束', event.description, 'warning');
        // 考试结束，可以触发自动交卷逻辑
        this.handleExamEnded();
        break;

      case ExamWebSocketClient.EventType.EXAM_WARNING:
        this.showNotification('考试提醒', event.description, 'info');
        break;

      case ExamWebSocketClient.EventType.EXAM_TIME_REMAINING:
        this.showNotification('剩余时间', event.description, 'info');
        break;

      case ExamWebSocketClient.EventType.EXAM_FORCE_SUBMIT:
        this.showNotification('强制交卷', event.description, 'error');
        this.handleForceSubmit();
        break;

      default:
        console.log('收到考试事件:', event);
    }
  }

  /**
   * 处理考试结束
   */
  private handleExamEnded(): void {
    // 这里可以触发自动交卷逻辑
    console.log('考试结束，准备自动交卷...');
    // 触发自定义事件或回调
    const event = new CustomEvent('exam-ended', {
      detail: { examId: this.options.examId }
    });
    window.dispatchEvent(event);
  }

  /**
   * 处理强制交卷
   */
  private handleForceSubmit(): void {
    console.log('强制交卷，立即提交试卷...');
    // 触发强制交卷事件
    const event = new CustomEvent('exam-force-submit', {
      detail: { examId: this.options.examId }
    });
    window.dispatchEvent(event);
  }

  /**
   * 显示通知
   */
  private showNotification(title: string, message: string, type: 'info' | 'warning' | 'error'): void {
    console.log(`[${type}] ${title}: ${message}`);

    // 使用浏览器通知API
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, {
        body: message,
        icon: '/favicon.ico'
      });
    }

    // 也可以在页面上显示通知
    this.dispatchNotificationEvent(title, message, type);
  }

  /**
   * 分发通知事件
   */
  private dispatchNotificationEvent(title: string, message: string, type: string): void {
    const event = new CustomEvent('websocket-notification', {
      detail: { title, message, type, timestamp: new Date() }
    });
    window.dispatchEvent(event);
  }

  /**
   * 处理错误
   */
  private handleError(error: any): void {
    console.error('WebSocket错误:', error);
    this.isConnecting = false;

    if (this.options.onError) {
      this.options.onError(error);
    }
  }

  /**
   * 处理连接关闭
   */
  private handleClose(event: CloseEvent): void {
    console.log('WebSocket连接关闭:', event.code, event.reason);
    this.isConnecting = false;

    // 触发断开连接回调
    if (this.options.onDisconnected) {
      this.options.onDisconnected();
    }

    // 尝试重连（如果不是正常关闭）
    if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);

      setTimeout(() => {
        this.connect();
      }, this.reconnectInterval);
    }
  }

  /**
   * 获取连接状态
   */
  public getConnectionState(): string {
    if (!this.socket) {
      return 'DISCONNECTED';
    }

    switch (this.socket.readyState) {
      case WebSocket.CONNECTING:
        return 'CONNECTING';
      case WebSocket.OPEN:
        return 'CONNECTED';
      case WebSocket.CLOSING:
        return 'CLOSING';
      case WebSocket.CLOSED:
        return 'DISCONNECTED';
      default:
        return 'UNKNOWN';
    }
  }

  /**
   * 检查是否已连接
   */
  public isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN;
  }
}

/**
 * 创建考试WebSocket客户端
 */
export function createExamWebSocket(options: ExamWebSocketOptions): ExamWebSocketClient {
  return new ExamWebSocketClient(options);
}