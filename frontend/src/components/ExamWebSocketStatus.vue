<template>
  <div class="exam-websocket-status">
    <!-- 连接状态指示器 -->
    <div class="status-indicator" :class="connectionState.toLowerCase()">
      <el-tooltip :content="connectionStateText" placement="top">
        <div class="status-dot"></div>
      </el-tooltip>
      <span class="status-text">{{ connectionStateText }}</span>
    </div>

    <!-- 通知按钮和面板 -->
    <div class="notifications-container">
      <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0">
        <el-button
          type="primary"
          :icon="Bell"
          circle
          @click="toggleNotificationsPanel"
        />
      </el-badge>

      <!-- 通知面板 -->
      <div v-if="showNotificationsPanel" class="notifications-panel">
        <div class="notifications-header">
          <h3>系统通知 ({{ notifications.length }})</h3>
          <div class="header-actions">
            <el-button
              type="text"
              :disabled="notifications.length === 0"
              @click="clearNotifications"
            >
              清空
            </el-button>
            <el-button
              type="text"
              icon="Close"
              @click="closeNotificationsPanel"
            />
          </div>
        </div>

        <div class="notifications-list" v-if="notifications.length > 0">
          <div
            v-for="(notification, index) in notifications"
            :key="index"
            class="notification-item"
            :class="notification.type"
          >
            <div class="notification-header">
              <span class="notification-title">{{ notification.title }}</span>
              <span class="notification-time">
                {{ formatTime(notification.timestamp) }}
              </span>
            </div>
            <div class="notification-content">
              {{ notification.message }}
            </div>
          </div>
        </div>

        <div v-else class="empty-notifications">
          <el-empty description="暂无通知" :image-size="80" />
        </div>
      </div>
    </div>

    <!-- 考试事件时间线 -->
    <div class="exam-events-container" v-if="examEvents.length > 0">
      <el-popover
        placement="bottom-end"
        title="考试事件时间线"
        :width="400"
        trigger="click"
      >
        <template #reference>
          <el-button type="info" :icon="Clock" circle />
        </template>

        <div class="exam-events-timeline">
          <div
            v-for="(event, index) in examEvents"
            :key="index"
            class="event-item"
          >
            <div class="event-time">
              {{ formatTime(new Date(event.eventTime)) }}
            </div>
            <div class="event-content">
              <div class="event-type">{{ getEventTypeText(event.eventType) }}</div>
              <div class="event-description">{{ event.description }}</div>
              <div v-if="event.extraData" class="event-extra">
                <small>额外信息: {{ JSON.stringify(event.extraData) }}</small>
              </div>
            </div>
          </div>
        </div>

        <div class="timeline-footer">
          <el-button
            type="text"
            size="small"
            @click="clearExamEvents"
            :disabled="examEvents.length === 0"
          >
            清空事件记录
          </el-button>
        </div>
      </el-popover>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue';
import { Bell, Clock } from '@element-plus/icons-vue';
import { useWebSocket } from '@/hooks/useWebSocket';
import type { ExamWebSocketOptions } from '@/utils/websocket';

interface Props {
  examId: number;
  userId: number;
}

const props = defineProps<Props>();

// 通知面板显示状态
const showNotificationsPanel = ref(false);

// 创建WebSocket连接
const wsOptions: ExamWebSocketOptions = {
  examId: props.examId,
  userId: props.userId,
  onExamEvent: (event) => {
    // 处理重要考试事件
    handleImportantExamEvent(event);
  }
};

const {
  isConnected,
  connectionState,
  notifications,
  examEvents,
  connect,
  disconnect,
  clearNotifications,
  clearExamEvents
} = useWebSocket(wsOptions);

// 计算属性
const connectionStateText = computed(() => {
  const stateMap: Record<string, string> = {
    CONNECTED: '已连接',
    CONNECTING: '连接中',
    DISCONNECTED: '未连接',
    CLOSING: '关闭中',
    UNKNOWN: '未知状态'
  };
  return stateMap[connectionState.value] || connectionState.value;
});

const unreadCount = computed(() => {
  return notifications.length;
});

// 格式化时间
const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

// 获取事件类型文本
const getEventTypeText = (eventType: string) => {
  const typeMap: Record<string, string> = {
    EXAM_STARTED: '考试开始',
    EXAM_ENDED: '考试结束',
    EXAM_WARNING: '考试提醒',
    EXAM_PAUSED: '考试暂停',
    EXAM_RESUMED: '考试恢复',
    EXAM_FORCE_SUBMIT: '强制交卷',
    EXAM_TIME_REMAINING: '剩余时间',
    EXAM_STUDENT_JOINED: '学生加入',
    EXAM_STUDENT_LEFT: '学生离开'
  };
  return typeMap[eventType] || eventType;
};

// 处理重要考试事件
const handleImportantExamEvent = (event: any) => {
  switch (event.eventType) {
    case 'EXAM_ENDED':
      // 考试结束，显示重要提醒
      console.log('考试已结束，请及时交卷');
      break;
    case 'EXAM_FORCE_SUBMIT':
      // 强制交卷
      console.log('系统将强制交卷');
      break;
  }
};

// 切换通知面板
const toggleNotificationsPanel = () => {
  showNotificationsPanel.value = !showNotificationsPanel.value;
};

// 关闭通知面板
const closeNotificationsPanel = () => {
  showNotificationsPanel.value = false;
};

// 组件挂载时连接WebSocket
onMounted(() => {
  connect();
});

// 组件卸载时断开连接
onUnmounted(() => {
  disconnect();
});
</script>

<style scoped>
.exam-websocket-status {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

/* 状态指示器 */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-indicator.connected .status-dot {
  background-color: #67c23a;
  box-shadow: 0 0 6px #67c23a;
}

.status-indicator.connecting .status-dot {
  background-color: #e6a23c;
  box-shadow: 0 0 6px #e6a23c;
  animation: pulse 1.5s infinite;
}

.status-indicator.disconnected .status-dot {
  background-color: #f56c6c;
  box-shadow: 0 0 6px #f56c6c;
}

.status-text {
  font-weight: 500;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 通知容器 */
.notifications-container {
  position: relative;
}

.notifications-panel {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  width: 350px;
  max-height: 500px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}

.notifications-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #fafafa;
}

.notifications-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notifications-list {
  max-height: 400px;
  overflow-y: auto;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #f9f9f9;
}

.notification-item.info {
  border-left: 3px solid #409eff;
}

.notification-item.warning {
  border-left: 3px solid #e6a23c;
}

.notification-item.error {
  border-left: 3px solid #f56c6c;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.notification-title {
  font-weight: 600;
  font-size: 13px;
}

.notification-time {
  font-size: 11px;
  color: #909399;
}

.notification-content {
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}

.empty-notifications {
  padding: 40px 20px;
  text-align: center;
}

/* 考试事件时间线 */
.exam-events-container {
  margin-left: auto;
}

.exam-events-timeline {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
}

.event-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.event-item:last-child {
  border-bottom: none;
}

.event-time {
  flex-shrink: 0;
  width: 80px;
  font-size: 11px;
  color: #909399;
  font-family: monospace;
}

.event-content {
  flex: 1;
}

.event-type {
  font-weight: 600;
  font-size: 12px;
  color: #409eff;
  margin-bottom: 4px;
}

.event-description {
  font-size: 12px;
  color: #606266;
}

.event-extra {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
}

.timeline-footer {
  padding: 8px;
  border-top: 1px solid #e4e7ed;
  text-align: center;
}
</style>