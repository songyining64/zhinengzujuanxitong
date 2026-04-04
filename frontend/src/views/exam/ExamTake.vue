<template>
  <div class="exam-take" v-loading="pageLoading">
    <template v-if="summary && summary.recordStatus === 'SUBMITTED'">
      <el-result icon="success" title="已交卷" :sub-title="summary.examTitle">
        <template #extra>
          <p v-if="summary.totalScore != null">得分：<strong>{{ summary.totalScore }}</strong></p>
          <p v-if="summary.passScore != null">及格线：{{ summary.passScore }}</p>
          <p v-if="summary.passed != null">是否及格：{{ summary.passed ? '是' : '否' }}</p>
          <p v-if="showRank">
            排名：第 {{ summary.rank }} / {{ summary.rankTotal }} 名
          </p>
          <el-button type="primary" @click="$router.push('/exam/take')">返回考试列表</el-button>
        </template>
      </el-result>
    </template>

    <template v-else-if="summary && questions.length">
      <el-affix :offset="0">
        <div class="exam-toolbar">
          <div class="toolbar-left">
            <h2 class="title">{{ summary.examTitle }}</h2>
            <span class="meta">切屏 {{ summary.switchBlurCount ?? 0 }} 次</span>
            <span v-if="summary.switchBlurLimit" class="meta warn">
              上限 {{ summary.switchBlurLimit }} 次，超限将自动交卷
            </span>
          </div>
          <div class="toolbar-right">
            <span class="save-hint" v-if="lastSavedText">自动保存 {{ lastSavedText }}</span>
            <span class="timer" :class="{ danger: remainSeconds <= 300 && remainSeconds > 0 }">
              剩余时间 {{ remainText }}
            </span>
            <el-button type="primary" :loading="submitting" @click="onSubmit">交卷</el-button>
          </div>
        </div>
      </el-affix>

      <el-row :gutter="20" class="exam-body">
        <el-col :xs="24" :sm="24" :md="7" :lg="6" class="nav-col">
          <el-card shadow="never" class="nav-card">
            <template #header>
              <span>题号导航</span>
              <el-text size="small" type="info">{{ currentIdx + 1 }} / {{ questions.length }}</el-text>
            </template>
            <div class="nav-grid">
              <el-button
                v-for="(q, i) in questions"
                :key="q.questionId"
                :type="i === currentIdx ? 'primary' : isAnswered(q.questionId) ? 'success' : 'default'"
                size="small"
                class="nav-btn"
                @click="currentIdx = i"
              >
                {{ q.orderNo }}
              </el-button>
            </div>
            <p class="nav-legend">
              <span class="dot primary" /> 当前
              <span class="dot success" /> 已答
            </p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="17" :lg="18" class="answer-col">
          <el-card v-if="currentQ" class="q-card" shadow="never">
            <div class="q-head">
              <span class="no">{{ currentQ.orderNo }}.</span>
              <el-tag size="small" type="info">{{ typeLabel(currentQ.type) }}</el-tag>
              <span class="score">（{{ currentQ.score }} 分）</span>
            </div>
            <div class="stem" v-html="stemHtml(currentQ.stem)"></div>

            <el-radio-group
              v-if="currentQ.type === 'SINGLE' || currentQ.type === 'TRUE_FALSE'"
              v-model="answers[currentQ.questionId]"
              class="opt-block"
              @change="scheduleDebouncedSave"
            >
              <el-radio
                v-for="(opt, idx) in parseOptions(currentQ.optionsJson)"
                :key="idx"
                :label="letter(idx)"
              >
                {{ letter(idx) }}. {{ opt }}
              </el-radio>
            </el-radio-group>

            <div v-else-if="currentQ.type === 'MULTIPLE'" class="opt-block">
              <el-checkbox-group
                :model-value="multiModel(currentQ.questionId)"
                @change="(v: string[]) => onMultiChange(currentQ.questionId, v)"
              >
                <el-checkbox
                  v-for="(opt, idx) in parseOptions(currentQ.optionsJson)"
                  :key="idx"
                  :label="letter(idx)"
                >
                  {{ letter(idx) }}. {{ opt }}
                </el-checkbox>
              </el-checkbox-group>
            </div>

            <el-input
              v-else-if="currentQ.type === 'FILL'"
              v-model="answers[currentQ.questionId]"
              placeholder="请填写答案"
              @input="scheduleDebouncedSave"
            />

            <el-input
              v-else-if="currentQ.type === 'SHORT'"
              v-model="answers[currentQ.questionId]"
              type="textarea"
              :rows="8"
              placeholder="请作答"
              @input="scheduleDebouncedSave"
            />

            <div v-else class="unknown-type">暂不支持的题型：{{ currentQ.type }}</div>

            <div class="q-foot">
              <el-button :disabled="currentIdx <= 0" @click="currentIdx--">上一题</el-button>
              <el-button :disabled="currentIdx >= questions.length - 1" type="primary" @click="currentIdx++">
                下一题
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <el-empty v-else-if="!pageLoading" description="无法加载试卷" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '@/api/http';
import { useExamAntiCheat } from '@/hooks/useExamAntiCheat';
import type { TakeQuestion, ExamRecordSummary } from '@/types/exam';

const route = useRoute();
const router = useRouter();

const recordId = computed(() => Number(route.params.recordId));
const recordIdRef = ref<number | null>(null);

const pageLoading = ref(true);
const submitting = ref(false);
const saving = ref(false);
const summary = ref<ExamRecordSummary | null>(null);
const questions = ref<TakeQuestion[]>([]);
const answers = reactive<Record<number, string>>({});
const lastSavedAt = ref<number | null>(null);
const remainSeconds = ref(0);
const currentIdx = ref(0);

const currentQ = computed(() => {
  const qs = questions.value;
  if (!qs.length) return null;
  const i = Math.min(Math.max(currentIdx.value, 0), qs.length - 1);
  return qs[i];
});

function isAnswered(qid: number) {
  const v = answers[qid];
  return v != null && String(v).trim() !== '';
}

let tickTimer: ReturnType<typeof setInterval> | null = null;
let autoSaveTimer: ReturnType<typeof setInterval> | null = null;
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

const showRank = computed(() => {
  const s = summary.value;
  if (!s) return false;
  return s.rank != null && s.rankTotal != null;
});

const remainText = computed(() => {
  const s = remainSeconds.value;
  if (s <= 0) return '00:00:00';
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  return [h, m, sec].map((n) => String(n).padStart(2, '0')).join(':');
});

const lastSavedText = computed(() => {
  if (!lastSavedAt.value) return '';
  const d = new Date(lastSavedAt.value);
  return `已保存 ${d.toLocaleTimeString()}`;
});

watch(
  () => recordId.value,
  (v) => {
    recordIdRef.value = Number.isFinite(v) ? v : null;
  },
  { immediate: true }
);
useExamAntiCheat(recordIdRef);

function typeLabel(t: string) {
  const map: Record<string, string> = {
    SINGLE: '单选题',
    MULTIPLE: '多选题',
    TRUE_FALSE: '判断题',
    FILL: '填空题',
    SHORT: '简答题'
  };
  return map[t] || t;
}

function letter(idx: number) {
  return String.fromCharCode(65 + idx);
}

function parseOptions(json: string | null): string[] {
  if (!json) return [];
  try {
    const arr = JSON.parse(json) as unknown;
    if (Array.isArray(arr)) {
      return arr.map((x, i) => {
        if (typeof x === 'string') return x;
        if (x && typeof x === 'object' && 'text' in (x as object)) {
          return String((x as { text: unknown }).text);
        }
        return `选项${i + 1}`;
      });
    }
  } catch {
    return [];
  }
  return [];
}

function stemHtml(stem: string) {
  if (!stem) return '';
  if (/<[a-z][\s\S]*>/i.test(stem)) return stem;
  return stem.replace(/\n/g, '<br/>');
}

function multiModel(qid: number): string[] {
  const raw = answers[qid];
  if (!raw) return [];
  return raw.split(/[,，;；\s]+/).filter(Boolean);
}

function onMultiChange(qid: number, vals: string[]) {
  answers[qid] = [...vals].sort().join(',');
  scheduleDebouncedSave();
}

function tickDeadline() {
  const s = summary.value;
  if (!s?.deadlineAt) {
    remainSeconds.value = 0;
    return;
  }
  const end = new Date(s.deadlineAt).getTime();
  remainSeconds.value = Math.max(0, Math.floor((end - Date.now()) / 1000));
}

async function loadAll() {
  pageLoading.value = true;
  try {
    const rid = recordId.value;
    if (!Number.isFinite(rid)) {
      ElMessage.error('无效的答卷');
      return;
    }
    const [sumRes, qRes] = await Promise.all([
      http.get<ExamRecordSummary>(`/api/exam/record/${rid}/summary`),
      http.get<TakeQuestion[]>(`/api/exam/record/${rid}/questions`)
    ]);
    summary.value = sumRes.data;
    questions.value = qRes.data || [];
    currentIdx.value = 0;

    for (const q of questions.value) {
      if (q.userAnswer != null && q.userAnswer !== '') {
        answers[q.questionId] = q.userAnswer;
      } else {
        answers[q.questionId] = '';
      }
    }

    if (summary.value?.recordStatus === 'SUBMITTED') {
      return;
    }

    tickDeadline();
    if (tickTimer) clearInterval(tickTimer);
    tickTimer = setInterval(tickDeadline, 1000);

    if (autoSaveTimer) clearInterval(autoSaveTimer);
    autoSaveTimer = setInterval(() => {
      void saveDraft(true);
    }, 30000);
  } finally {
    pageLoading.value = false;
  }
}

async function saveDraft(silent = false) {
  const rid = recordId.value;
  if (!summary.value || summary.value.recordStatus !== 'IN_PROGRESS') return;
  if (saving.value) return;
  const payload = questions.value.map((q) => ({
    questionId: q.questionId,
    userAnswer: answers[q.questionId] ?? ''
  }));
  saving.value = true;
  try {
    await http.post(`/api/exam/record/${rid}/save-answers`, { answers: payload });
    lastSavedAt.value = Date.now();
    if (!silent) {
      ElMessage.success('草稿已保存');
    }
  } catch {
    /* 全局已提示 */
  } finally {
    saving.value = false;
  }
}

function scheduleDebouncedSave() {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    debounceTimer = null;
    void saveDraft(true);
  }, 8000);
}

async function onSubmit() {
  const rid = recordId.value;
  try {
    await ElMessageBox.confirm('确定交卷？交卷后不能再修改。', '交卷确认', {
      type: 'warning'
    });
  } catch {
    return;
  }
  submitting.value = true;
  try {
    const payload = questions.value.map((q) => ({
      questionId: q.questionId,
      userAnswer: answers[q.questionId] ?? ''
    }));
    await http.post(`/api/exam/record/${rid}/submit`, { answers: payload });
    ElMessage.success('交卷成功');
    await loadAll();
  } catch {
    /* */
  } finally {
    submitting.value = false;
  }
}

onMounted(loadAll);

onBeforeUnmount(() => {
  if (tickTimer) clearInterval(tickTimer);
  if (autoSaveTimer) clearInterval(autoSaveTimer);
  if (debounceTimer) clearTimeout(debounceTimer);
});
</script>

<style scoped>
.exam-take {
  max-width: 1200px;
  margin: 0 auto;
}

.exam-body {
  margin-top: 8px;
}

.nav-card :deep(.el-card__header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nav-btn {
  min-width: 40px;
  padding: 8px 10px;
}

.nav-legend {
  margin: 12px 0 0;
  font-size: 12px;
  color: #909399;
}

.nav-legend .dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  margin: 0 4px 0 10px;
  vertical-align: middle;
}
.nav-legend .dot.primary {
  background: #409eff;
}
.nav-legend .dot.success {
  background: #67c23a;
}

.answer-col .q-card {
  min-height: 360px;
}

.q-foot {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.exam-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.toolbar-left .title {
  margin: 0 0 8px;
  font-size: 18px;
}

.meta {
  font-size: 12px;
  color: #909399;
  margin-right: 12px;
}

.meta.warn {
  color: #e6a23c;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.save-hint {
  font-size: 12px;
  color: #67c23a;
}

.timer {
  font-family: ui-monospace, monospace;
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.timer.danger {
  color: #f56c6c;
}

.question-list {
  padding: 16px 0 48px;
}

.q-card {
  margin-bottom: 16px;
}

.q-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.q-head .no {
  font-weight: 600;
}

.stem {
  margin-bottom: 12px;
  line-height: 1.6;
  color: #303133;
}

.opt-block {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.opt-block :deep(.el-radio),
.opt-block :deep(.el-checkbox) {
  margin-right: 0;
  height: auto;
  white-space: normal;
  align-items: flex-start;
}

.unknown-type {
  color: #f56c6c;
}
</style>
