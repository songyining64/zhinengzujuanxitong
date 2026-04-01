<template>
  <div class="exam-websocket-admin-panel">
    <el-card class="panel-card">
      <template #header>
        <div class="card-header">
          <h3>考试WebSocket控制面板</h3>
          <el-tag :type="connectionState === 'CONNECTED' ? 'success' : 'danger'">
            {{ connectionState === 'CONNECTED' ? '已连接' : '未连接' }}
          </el-tag>
        </div>
      </template>

      <!-- 考试选择 -->
      <div class="section">
        <h4>选择考试</h4>
        <el-select
          v-model="selectedExam"
          placeholder="请选择考试"
          filterable
          style="width: 100%"
          @change="onExamChange"
        >
          <el-option
            v-for="exam in examList"
            :key="exam.id"
            :label="exam.title"
            :value="exam.id"
          >
            <span>{{ exam.title }}</span>
            <span style="float: right; color: #8492a6; font-size: 13px">
              {{ exam.status }}
            </span>
          </el-option>
        </el-select>
      </div>

      <!-- 在线状态 -->
      <div class="section" v-if="selectedExam">
        <h4>在线状态</h4>
        <div class="online-status">
          <el-statistic title="在线人数" :value="onlineCount || 0" />
          <el-button
            type="primary"
            size="small"
            :loading="loadingOnlineCount"
            @click="refreshOnlineCount"
          >
            刷新
          </el-button>
        </div>
      </div>

      <!-- 考试控制 -->
      <div class="section" v-if="selectedExam">
        <h4>考试控制</h4>
        <div class="control-buttons">
          <el-button
            type="primary"
            :icon="VideoPlay"
            @click="triggerExamStart"
            :loading="loadingStart"
          >
            发送开始通知
          </el-button>

          <el-button
            type="warning"
            :icon="VideoPause"
            @click="triggerExamEnd"
            :loading="loadingEnd"
          >
            发送结束通知
          </el-button>

          <el-button
            type="info"
            :icon="Bell"
            @click="showWarningDialog = true"
          >
            发送提醒
          </el-button>
        </div>
      </div>

      <!-- 消息广播 -->
      <div class="section" v-if="selectedExam">
        <h4>消息广播</h4>
        <div class="broadcast-section">
          <el-input
            v-model="broadcastMessage"
            placeholder="输入要广播的消息"
            style="margin-bottom: 12px"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            @click="sendBroadcast"
            :disabled="!broadcastMessage.trim()"
          >
            广播消息
          </el-button>
        </div>
      </div>

      <!-- 用户消息 -->
      <div class="section" v-if="selectedExam">
        <h4>发送用户消息</h4>
        <div class="user-message-section">
          <el-input
            v-model="targetUserId"
            placeholder="用户ID"
            style="width: 150px; margin-right: 12px"
          />
          <el-input
            v-model="userMessage"
            placeholder="输入要发送的消息"
            style="flex: 1; margin-right: 12px"
          />
          <el-button
            type="primary"
            :icon="Message"
            @click="sendUserMessage"
            :disabled="!targetUserId || !userMessage.trim()"
          >
            发送
          </el-button>
        </div>
      </div>

      <!-- 连接日志 -->
      <div class="section">
        <h4>操作日志</h4>
        <div class="log-container">
          <div
            v-for="(log, index) in operationLogs"
            :key="index"
            class="log-item"
            :class="log.type"
          >
            <span class="log-time">{{ formatTime(log.timestamp) }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
          <div v-if="operationLogs.length === 0" class="empty-log">
            暂无操作记录
          </div>
        </div>
        <el-button
          type="text"
          size="small"
          @click="clearLogs"
          :disabled="operationLogs.length === 0"
        >
          清空日志
        </el-button>
      </div>
    </el-card>

    <!-- 考试提醒对话框 -->
    <el-dialog
      v-model="showWarningDialog"
      title="发送考试提醒"
      width="400px"
    >
      <el-form :model="warningForm" label-width="80px">
        <el-form-item label="提醒消息">
          <el-input
            v-model="warningForm.message"
            placeholder="例如：距离考试结束还有5分钟"
          />
        </el-form-item>
        <el-form-item label="剩余分钟">
          <el-input-number
            v-model="warningForm.remainingMinutes"
            :min="1"
            :max="300"
            placeholder="剩余分钟数"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showWarningDialog = false">取消</el-button>
          <el-button type="primary" @click="sendExamWarning">
            发送提醒
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import {
  VideoPlay,
  VideoPause,
  Bell,
  Promotion,
  Message
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useWebSocket } from '@/hooks/useWebSocket';
import type { ExamWebSocketOptions } from '@/utils/websocket';

// 考试数据
interface Exam {
  id: number;
  title: string;
  status: string;
  startTime: string;
  endTime: string;
}

// 操作日志
interface OperationLog {
  timestamp: Date;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
}

// 响应式数据
const selectedExam = ref<number | null>(null);
const examList = ref<Exam[]>([
  { id: 1, title: '期中考试 - 数学', status: '进行中', startTime: '2024-01-01 09:00', endTime: '2024-01-01 11:00' },
  { id: 2, title: '期末考试 - 语文', status: '已结束', startTime: '2024-01-02 09:00', endTime: '2024-01-02 11:00' },
  { id: 3, title: '模拟考试 - 英语', status: '未开始', startTime: '2024-01-03 09:00', endTime: '2024-01-03 11:00' }
]);

const onlineCount = ref(0);
const loadingOnlineCount = ref(false);

const broadcastMessage = ref('');
const targetUserId = ref('');
const userMessage = ref('');

const showWarningDialog = ref(false);
const warningForm = reactive({
  message: '距离考试结束还有5分钟，请及时检查并提交试卷',
  remainingMinutes: 5
});

const loadingStart = ref(false);
const loadingEnd = ref(false);

const operationLogs = reactive<OperationLog[]>([]);

// 模拟管理员用户ID
const adminUserId = 1;

// 创建WebSocket连接（用于接收通知）
const wsOptions: ExamWebSocketOptions = {
  examId: 0, // 初始值，会在考试选择后更新
  userId: adminUserId,
  onExamEvent: (event) => {
    addLog(`收到考试事件: ${event.description}`, 'info');
  },
  onMessage: (message) => {
    addLog(`收到系统消息: ${message.content}`, 'info');
  }
};

const {
  isConnected,
  connectionState,
  connect,
  disconnect
} = useWebSocket(wsOptions);

// 计算属性
const isExamSelected = computed(() => selectedExam.value !== null);

// 格式化时间
const formatTime = (date: Date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

// 添加操作日志
const addLog = (message: string, type: OperationLog['type'] = 'info') => {
  operationLogs.unshift({
    timestamp: new Date(),
    message,
    type
  });

  // 限制日志数量
  if (operationLogs.length > 50) {
    operationLogs.splice(50);
  }
};

// 清空日志
const clearLogs = () => {
  operationLogs.splice(0, operationLogs.length);
  ElMessage.success('日志已清空');
};

// 考试选择变化
const onExamChange = (examId: number) => {
  addLog(`选择了考试 ID: ${examId}`, 'info');
  // 这里应该重新连接WebSocket到新的考试房间
  // 实际项目中需要实现重新连接逻辑
};

// 刷新在线人数
const refreshOnlineCount = async () => {
  if (!selectedExam.value) return;

  loadingOnlineCount.value = true;
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000));
    onlineCount.value = Math.floor(Math.random() * 50) + 10; // 模拟数据
    addLog(`刷新在线人数: ${onlineCount.value}`, 'success');
  } catch (error) {
    addLog(`获取在线人数失败: ${error}`, 'error');
    ElMessage.error('获取在线人数失败');
  } finally {
    loadingOnlineCount.value = false;
  }
};

// 触发考试开始通知
const triggerExamStart = async () => {
  if (!selectedExam.value) return;

  loadingStart.value = true;
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000));

    const exam = examList.value.find(e => e.id === selectedExam.value);
    if (exam) {
      addLog(`已发送考试开始通知: ${exam.title}`, 'success');
      ElMessage.success('考试开始通知已发送');
    }
  } catch (error) {
    addLog(`发送考试开始通知失败: ${error}`, 'error');
    ElMessage.error('发送失败');
  } finally {
    loadingStart.value = false;
  }
};

// 触发考试结束通知
const triggerExamEnd = async () => {
  if (!selectedExam.value) return;

  await ElMessageBox.confirm(
    '确定要发送考试结束通知吗？所有学生将收到考试结束提醒。',
    '确认操作',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  );

  loadingEnd.value = true;
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000));

    const exam = examList.value.find(e => e.id === selectedExam.value);
    if (exam) {
      addLog(`已发送考试结束通知: ${exam.title}`, 'success');
      ElMessage.success('考试结束通知已发送');
    }
  } catch (error) {
    addLog(`发送考试结束通知失败: ${error}`, 'error');
    ElMessage.error('发送失败');
  } finally {
    loadingEnd.value = false;
  }
};

// 发送考试提醒
const sendExamWarning = async () => {
  if (!selectedExam.value) return;

  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000));

    addLog(`已发送考试提醒: ${warningForm.message}`, 'success');
    ElMessage.success('考试提醒已发送');
    showWarningDialog.value = false;

    // 重置表单
    warningForm.message = '距离考试结束还有5分钟，请及时检查并提交试卷';
    warningForm.remainingMinutes = 5;
  } catch (error) {
    addLog(`发送考试提醒失败: ${error}`, 'error');
    ElMessage.error('发送失败');
  }
};

// 发送广播消息
const sendBroadcast = async () => {
  if (!selectedExam.value || !broadcastMessage.value.trim()) return;

  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000));

    addLog(`已广播消息: ${broadcastMessage.value}`, 'success');
    ElMessage.success('消息已广播');
    broadcastMessage.value = '';
  } catch (error) {
    addLog(`广播消息失败: ${error}`, 'error');
    ElMessage.error('发送失败');
  }
};

// 发送用户消息
const sendUserMessage = async () => {
  if (!selectedExam.value || !targetUserId.value || !userMessage.value.trim()) return;

  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000));

    addLog(`已向用户 ${targetUserId.value} 发送消息: ${userMessage.value}`, 'success');
    ElMessage.success('消息已发送');
    userMessage.value = '';
  } catch (error) {
    addLog(`发送用户消息失败: ${error}`, 'error');
    ElMessage.error('发送失败');
  }
};

// 组件挂载时连接WebSocket
onMounted(() => {
  connect();
  addLog('WebSocket控制面板已初始化', 'info');
});
</script>

<style scoped>
.exam-websocket-admin-panel {
  max-width: 800px;
  margin: 0 auto;
}

.panel-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section {
  margin-bottom: 24px;
}

.section h4 {
  margin-bottom: 12px;
  color: #303133;
  font-weight: 600;
}

.online-status {
  display: flex;
  align-items: center;
  gap: 16px;
}

.control-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.broadcast-section {
  display: flex;
  flex-direction: column;
}

.user-message-section {
  display: flex;
  align-items: center;
}

.log-container {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
  background-color: #fafafa;
  margin-bottom: 8px;
}

.log-item {
  padding: 6px 8px;
  border-bottom: 1px solid #f0f0f0;
  font-family: monospace;
  font-size: 12px;
}

.log-item:last-child {
  border-bottom: none;
}

.log-item.info {
  color: #606266;
}

.log-item.success {
  color: #67c23a;
}

.log-item.warning {
  color: #e6a23c;
}

.log-item.error {
  color: #f56c6c;
}

.log-time {
  display: inline-block;
  width: 80px;
  color: #909399;
}

.log-message {
  margin-left: 12px;
}

.empty-log {
  text-align: center;
  color: #909399;
  padding: 16px;
}
</style>