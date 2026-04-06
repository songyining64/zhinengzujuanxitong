<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">试卷浏览</span>
        <div class="header-actions">
          <span class="label">课程</span>
          <el-select
            v-model="selectedCourseId"
            placeholder="请选择课程"
            clearable
            filterable
            style="width: 260px"
            @change="onCourseChange"
          >
            <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
          </el-select>
        </div>
      </div>
    </template>

    <el-tabs v-model="tab">
      <el-tab-pane label="试卷列表" name="paper">
        <el-table :data="paperList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="试卷标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="mode" label="模式" width="100">
            <template #default="{ row }">{{ modeLabel(row.mode) }}</template>
          </el-table-column>
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPaperDetail(row)">详情</el-button>
              <el-button link type="primary" @click="openPaperPreview(row)">放大浏览</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="paperPage"
            v-model:page-size="paperSize"
            :total="paperTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="fetchPapers"
            @size-change="fetchPapers"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="组卷模板" name="template">
        <el-table :data="templateList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="模板名称" min-width="220" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column prop="updateTime" label="更新时间" width="170" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="生成日志" name="log">
        <el-table :data="logList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="paperId" label="试卷ID" width="90" />
          <el-table-column prop="mode" label="模式" width="100">
            <template #default="{ row }">{{ modeLabel(row.mode) }}</template>
          </el-table-column>
          <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
          <el-table-column prop="createTime" label="时间" width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="paperDetailVisible" title="试卷详情" width="760px">
      <template #header>
        <div class="detail-header">
          <span>试卷详情</span>
          <el-button
            v-if="paperDetail"
            type="primary"
            text
            @click="openPreviewFromDetail"
          >
            放大浏览
          </el-button>
        </div>
      </template>
      <el-descriptions v-if="paperDetail" :column="2" border>
        <el-descriptions-item label="试卷ID">{{ paperDetail.paper.id }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ paperDetail.paper.title }}</el-descriptions-item>
        <el-descriptions-item label="模式">{{ modeLabel(paperDetail.paper.mode) }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ paperDetail.paper.totalScore }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="paperDetail" :data="paperDetail.questions" stripe style="margin-top: 12px" max-height="360">
        <el-table-column prop="questionOrder" label="序号" width="70" />
        <el-table-column prop="questionId" label="题目ID" width="90" />
        <el-table-column prop="type" label="题型" width="100" />
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column prop="stem" label="题干" min-width="300" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="paperPreviewVisible" fullscreen append-to-body destroy-on-close>
      <template #header>
        <div class="preview-header">
          <div class="preview-title">{{ previewPaperTitle }}</div>
          <div class="preview-sub">A4 预览模式</div>
        </div>
      </template>
      <div v-if="paperDetail" class="a4-wrapper" v-loading="previewLoading">
        <div class="a4-page">
          <div class="a4-page-head">
            <h2>{{ paperDetail.paper.title }}</h2>
            <p>
              试卷ID：{{ paperDetail.paper.id }}　
              模式：{{ modeLabel(paperDetail.paper.mode) }}　
              总分：{{ paperDetail.paper.totalScore }}
            </p>
          </div>
          <div class="a4-question-list">
            <div v-for="q in paperDetail.questions" :key="q.paperQuestionId" class="a4-question-item">
              <div class="a4-q-title">
                {{ q.questionOrder }}. {{ typeLabel(q.type) }}（{{ q.score }}分）
              </div>
              <div class="a4-q-stem">{{ q.stem || '—' }}</div>
              <div v-if="showOptionBlock(q.type) && optionListByQuestionId(q.questionId).length" class="a4-options">
                <div v-for="(opt, idx) in optionListByQuestionId(q.questionId)" :key="idx" class="a4-option-item">
                  <span class="opt-key">{{ letter(idx) }}.</span>
                  <span>{{ opt }}</span>
                </div>
              </div>
              <div v-else-if="q.type === 'TRUE_FALSE'" class="tf-actions">
                <button type="button" class="tf-btn">✓ 对</button>
                <button type="button" class="tf-btn">✗ 错</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import http from '@/api/http';
import {
  fetchPaperDetail,
  fetchPaperGenerationLogs,
  fetchPaperPage,
  fetchPaperTemplates,
  type PaperDetailVO,
  type PaperGenerationLog,
  type PaperQuestionLine,
  type PaperRow,
  type PaperTemplate
} from '@/api/modules/paper';

interface CourseRow {
  id: number;
  name: string;
  code?: string;
}
interface CoursePage {
  records: CourseRow[];
  total: number;
}

const tab = ref('paper');
const selectedCourseId = ref<number | undefined>(undefined);
const courseList = ref<CourseRow[]>([]);

const templateList = ref<PaperTemplate[]>([]);
const paperList = ref<PaperRow[]>([]);
const paperPage = ref(1);
const paperSize = ref(10);
const paperTotal = ref(0);
const logList = ref<PaperGenerationLog[]>([]);

const paperDetailVisible = ref(false);
const paperDetail = ref<PaperDetailVO | null>(null);
const paperPreviewVisible = ref(false);
const previewLoading = ref(false);
const questionExtraMap = ref<Record<number, { options: string[] }>>({});

function courseLabel(c: CourseRow) {
  return c.code ? `${c.name}（${c.code}）` : c.name;
}
function modeLabel(v?: string) {
  if (v === 'AUTO') return '自动';
  if (v === 'TEMPLATE') return '模板';
  if (v === 'MANUAL') return '手动';
  return v || '-';
}
function letter(idx: number) {
  return String.fromCharCode(65 + idx);
}
function typeLabel(v?: string) {
  if (v === 'SINGLE') return '单选题';
  if (v === 'MULTIPLE') return '多选题';
  if (v === 'TRUE_FALSE') return '判断题';
  if (v === 'FILL') return '填空题';
  if (v === 'SHORT') return '简答题';
  return v || '题目';
}
function showOptionBlock(type?: string) {
  return type === 'SINGLE' || type === 'MULTIPLE';
}
function parseOptions(json?: string) {
  if (!json) return [] as string[];
  // 1) 优先按 JSON 数组解析（兼容 ["A选项","B选项"] / [{text:"..."}]）
  try {
    const arr = JSON.parse(json) as unknown;
    if (Array.isArray(arr)) {
      const parsed = arr
        .map((x) => {
          if (typeof x === 'string') return x;
          if (x && typeof x === 'object' && 'text' in (x as Record<string, unknown>)) {
            return String((x as Record<string, unknown>).text ?? '');
          }
          return '';
        })
        .map((s) => s.trim())
        .filter(Boolean);
      if (parsed.length) return parsed;
    }
  } catch {
    // 非 JSON 时走文本兜底
  }

  // 2) 兜底：按文本行解析（兼容 "A xxx\nB xxx" 或 "A. xxx; B. xxx"）
  const raw = json.trim();
  if (!raw) return [];
  let lines = raw
    .split(/\r?\n+/)
    .map((s) => s.trim())
    .filter(Boolean);

  if (lines.length <= 1) {
    lines = raw
      .split(/[;；]+/)
      .map((s) => s.trim())
      .filter(Boolean);
  }

  return lines.map((line) =>
    line.replace(/^[A-Ha-h][\.\)、\s]+/, '').trim()
  ).filter(Boolean);
}
function optionListByQuestionId(questionId: number) {
  return questionExtraMap.value[questionId]?.options ?? [];
}
const previewPaperTitle = computed(() => paperDetail.value?.paper?.title || '试卷放大浏览');

async function fetchCourses() {
  const { data } = await http.get<CoursePage>('/api/course', { params: { page: 1, size: 200 } });
  courseList.value = data?.records ?? [];
  if (!selectedCourseId.value && courseList.value.length) {
    selectedCourseId.value = courseList.value[0].id;
  }
}

async function fetchTemplates() {
  if (!selectedCourseId.value) {
    templateList.value = [];
    return;
  }
  templateList.value = await fetchPaperTemplates(selectedCourseId.value);
}

async function fetchPapers() {
  if (!selectedCourseId.value) {
    paperList.value = [];
    paperTotal.value = 0;
    return;
  }
  const page = await fetchPaperPage({ courseId: selectedCourseId.value, page: paperPage.value, size: paperSize.value });
  paperList.value = page?.records ?? [];
  paperTotal.value = page?.total ?? 0;
}

async function fetchLogs() {
  if (!selectedCourseId.value) {
    logList.value = [];
    return;
  }
  const page = await fetchPaperGenerationLogs({ courseId: selectedCourseId.value, page: 1, size: 20 });
  logList.value = page?.records ?? [];
}

async function onCourseChange() {
  paperPage.value = 1;
  await Promise.all([fetchTemplates(), fetchPapers(), fetchLogs()]);
}

async function openPaperDetail(row: PaperRow) {
  paperDetail.value = await fetchPaperDetail(row.id);
  paperDetailVisible.value = true;
}
async function openPaperPreview(row: PaperRow) {
  paperDetail.value = await fetchPaperDetail(row.id);
  await loadQuestionExtrasForPreview();
  paperPreviewVisible.value = true;
}
async function openPreviewFromDetail() {
  if (!paperDetail.value) return;
  await loadQuestionExtrasForPreview();
  paperPreviewVisible.value = true;
}

async function loadQuestionExtrasForPreview() {
  if (!paperDetail.value?.questions?.length) {
    questionExtraMap.value = {};
    return;
  }
  previewLoading.value = true;
  try {
    const ids = [...new Set(paperDetail.value.questions.map((q: PaperQuestionLine) => q.questionId))];
    const entries = await Promise.all(
      ids.map(async (qid) => {
        try {
          const { data } = await http.get<{ optionsJson?: string }>(`/api/question/${qid}`);
          return [qid, { options: parseOptions(data?.optionsJson) }] as const;
        } catch {
          return [qid, { options: [] as string[] }] as const;
        }
      })
    );
    questionExtraMap.value = Object.fromEntries(entries);
  } finally {
    previewLoading.value = false;
  }
}

onMounted(async () => {
  await fetchCourses();
  await Promise.all([fetchTemplates(), fetchPapers(), fetchLogs()]);
});
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.label {
  font-size: 13px;
  color: #606266;
}
.detail-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.preview-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.preview-title {
  font-size: 18px;
  font-weight: 600;
}
.preview-sub {
  font-size: 12px;
  color: #909399;
}
.a4-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 140px);
  overflow: auto;
}
.a4-page {
  width: 794px;
  min-height: 1123px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  padding: 44px 54px;
  box-sizing: border-box;
}
.a4-page-head h2 {
  margin: 0;
  text-align: center;
  font-size: 28px;
  letter-spacing: 1px;
}
.a4-page-head p {
  margin: 14px 0 0;
  text-align: center;
  color: #606266;
}
.a4-question-list {
  margin-top: 28px;
}
.a4-question-item {
  margin-bottom: 24px;
  page-break-inside: avoid;
}
.a4-q-title {
  font-size: 20px;
  font-weight: 600;
  line-height: 1.6;
}
.a4-q-stem {
  margin-top: 8px;
  font-size: 18px;
  line-height: 1.9;
  color: #303133;
  white-space: pre-wrap;
}
.a4-options {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.a4-option-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 16px;
  line-height: 1.7;
}
.opt-key {
  width: 24px;
  color: #606266;
}
.tf-actions {
  margin-top: 12px;
  display: flex;
  gap: 12px;
}
.tf-btn {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #303133;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 16px;
  cursor: default;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>

