<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">成绩统计</span>
        <div class="head-actions">
          <el-button :disabled="!selectedExamId" @click="refreshAll">刷新</el-button>
          <el-button type="primary" :disabled="!selectedExamId" @click="exportRank">导出排名</el-button>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <span class="label">课程</span>
      <el-select
        v-model="selectedCourseId"
        clearable
        filterable
        placeholder="请选择课程"
        style="width: 260px"
        @change="onCourseChange"
      >
        <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
      </el-select>

      <span class="label">考试</span>
      <el-select
        v-model="selectedExamId"
        clearable
        filterable
        placeholder="请选择考试"
        style="width: 320px"
        @change="onExamChange"
      >
        <el-option v-for="e in examList" :key="e.id" :label="examLabel(e)" :value="e.id" />
      </el-select>
    </div>

    <el-empty v-if="!selectedExamId" description="请选择考试后查看统计" />

    <template v-else>
      <div class="overview-grid" v-loading="overviewLoading">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">已交卷人数</div>
          <div class="kpi-value">{{ overview?.submittedCount ?? 0 }}</div>
        </el-card>
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">平均分</div>
          <div class="kpi-value">{{ num(overview?.avgScore) }}</div>
        </el-card>
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">最高分</div>
          <div class="kpi-value">{{ num(overview?.maxScore) }}</div>
        </el-card>
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">最低分</div>
          <div class="kpi-value">{{ num(overview?.minScore) }}</div>
        </el-card>
      </div>

      <el-row :gutter="12" class="overview-extra">
        <el-col :md="12" :sm="24">
          <el-card shadow="never" v-loading="overviewLoading">
            <template #header><span>通过情况</span></template>
            <div class="pass-block">
              <div>及格线：{{ num(overview?.passScore) || '未设置' }}</div>
              <div>通过人数：{{ overview?.passCount ?? 0 }}</div>
              <div>成绩发布：{{ overview?.scorePublished ? '已发布' : '未发布' }}</div>
              <el-progress :percentage="passRate" :stroke-width="14" />
            </div>
          </el-card>
        </el-col>
        <el-col :md="12" :sm="24">
          <el-card shadow="never" v-loading="rankLoading">
            <template #header><span>前 5 名</span></template>
            <el-table :data="rankTop5" size="small" max-height="220">
              <el-table-column prop="rank" label="名次" width="70" />
              <el-table-column label="姓名" min-width="120">
                <template #default="{ row }">{{ row.realName || row.username || row.studentId }}</template>
              </el-table-column>
              <el-table-column prop="totalScore" label="分数" width="90" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-tabs v-model="tab" class="main-tabs">
        <el-tab-pane label="成绩排名" name="rank">
          <el-table :data="rankList" stripe v-loading="rankLoading">
            <el-table-column prop="rank" label="名次" width="70" />
            <el-table-column prop="studentId" label="学号ID" width="100" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="totalScore" label="总分" width="90" />
            <el-table-column label="是否及格" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.passed === true" type="success" size="small">及格</el-tag>
                <el-tag v-else-if="row.passed === false" type="danger" size="small">不及格</el-tag>
                <el-tag v-else size="small">-</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="rankPage"
              v-model:page-size="rankSize"
              :total="rankTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="fetchRank"
              @size-change="fetchRank"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="题目统计" name="question">
          <el-table :data="questionStats" stripe v-loading="questionLoading">
            <el-table-column prop="questionId" label="题目ID" width="100" />
            <el-table-column prop="type" label="题型" width="100">
              <template #default="{ row }">{{ typeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="attemptCount" label="作答人数" width="100" />
            <el-table-column prop="correctCount" label="答对人数" width="100" />
            <el-table-column label="正确率" min-width="220">
              <template #default="{ row }">
                <div class="rate-cell">
                  <span>{{ pct(row.correctRate) }}</span>
                  <el-progress :percentage="toPercent(row.correctRate)" :stroke-width="10" />
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="知识点统计" name="kp">
          <el-table :data="kpStats" stripe v-loading="kpLoading">
            <el-table-column prop="knowledgePointId" label="知识点ID" width="110" />
            <el-table-column prop="knowledgePointName" label="知识点名称" min-width="180" />
            <el-table-column prop="questionCount" label="题目数" width="100" />
            <el-table-column label="平均正确率" min-width="220">
              <template #default="{ row }">
                <div class="rate-cell">
                  <span>{{ pct(row.avgCorrectRate) }}</span>
                  <el-progress :percentage="toPercent(row.avgCorrectRate)" :stroke-width="10" />
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import http from '@/api/http';

interface CourseRow {
  id: number;
  name: string;
  code?: string;
}
interface CoursePage {
  records: CourseRow[];
  total: number;
}
interface ExamRow {
  id: number;
  courseId: number;
  title: string;
  startTime: string;
  endTime: string;
}
interface ExamPage {
  records: ExamRow[];
  total: number;
}
interface ExamOverview {
  examId: number;
  submittedCount: number;
  totalRecords: number;
  avgScore: number | null;
  maxScore: number | null;
  minScore: number | null;
  passScore: number | null;
  scorePublished: boolean;
  passCount: number | null;
}
interface RankRow {
  rank: number;
  studentId: number;
  username?: string;
  realName?: string;
  totalScore?: number;
  passed?: boolean | null;
}
interface RankPage {
  records: RankRow[];
  total: number;
}
interface QuestionStatRow {
  questionId: number;
  type: string;
  attemptCount: number;
  correctCount: number;
  correctRate: number | null;
}
interface KpStatRow {
  knowledgePointId: number;
  knowledgePointName?: string;
  questionCount: number;
  avgCorrectRate: number | null;
}

const route = useRoute();
const tab = ref('rank');
const courseList = ref<CourseRow[]>([]);
const examList = ref<ExamRow[]>([]);
const selectedCourseId = ref<number | undefined>(undefined);
const selectedExamId = ref<number | undefined>(undefined);

const overview = ref<ExamOverview | null>(null);
const rankList = ref<RankRow[]>([]);
const rankTotal = ref(0);
const rankPage = ref(1);
const rankSize = ref(10);
const questionStats = ref<QuestionStatRow[]>([]);
const kpStats = ref<KpStatRow[]>([]);

const overviewLoading = ref(false);
const rankLoading = ref(false);
const questionLoading = ref(false);
const kpLoading = ref(false);

const passRate = computed(() => {
  const submitted = overview.value?.submittedCount ?? 0;
  if (!submitted) return 0;
  const passCount = overview.value?.passCount ?? 0;
  return Math.min(100, Math.max(0, Number(((passCount / submitted) * 100).toFixed(2))));
});
const rankTop5 = computed(() => rankList.value.slice(0, 5));

function courseLabel(c: CourseRow) {
  return c.code ? `${c.name}（${c.code}）` : c.name;
}
function examLabel(e: ExamRow) {
  return `${e.title}（${formatRange(e.startTime, e.endTime)}）`;
}
function formatRange(start?: string, end?: string) {
  const a = start?.replace('T', ' ').slice(0, 16) || '-';
  const b = end?.replace('T', ' ').slice(0, 16) || '-';
  return `${a} ~ ${b}`;
}
function num(v?: number | null) {
  if (v == null) return '';
  return Number(v).toFixed(2).replace(/\.00$/, '');
}
function pct(v?: number | null) {
  if (v == null) return '-';
  return `${(Number(v) * 100).toFixed(2)}%`;
}
function toPercent(v?: number | null) {
  if (v == null) return 0;
  return Math.min(100, Math.max(0, Number((Number(v) * 100).toFixed(2))));
}
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

async function fetchCourses() {
  const { data } = await http.get<CoursePage>('/api/course', { params: { page: 1, size: 200 } });
  courseList.value = data?.records ?? [];
}

async function fetchExams() {
  if (!selectedCourseId.value) {
    examList.value = [];
    selectedExamId.value = undefined;
    return;
  }
  const { data } = await http.get<ExamPage>('/api/exam/teacher', {
    params: { courseId: selectedCourseId.value, page: 1, size: 200 }
  });
  examList.value = data?.records ?? [];
  if (!examList.value.find((x) => x.id === selectedExamId.value)) {
    selectedExamId.value = examList.value[0]?.id;
  }
}

async function fetchOverview() {
  if (!selectedExamId.value) return;
  overviewLoading.value = true;
  try {
    const { data } = await http.get<ExamOverview>('/api/exam/analytics/overview', {
      params: { examId: selectedExamId.value }
    });
    overview.value = data || null;
  } finally {
    overviewLoading.value = false;
  }
}

async function fetchRank() {
  if (!selectedExamId.value) return;
  rankLoading.value = true;
  try {
    const { data } = await http.get<RankPage>('/api/exam/analytics/rank', {
      params: {
        examId: selectedExamId.value,
        page: rankPage.value,
        size: rankSize.value
      }
    });
    rankList.value = data?.records ?? [];
    rankTotal.value = data?.total ?? 0;
  } finally {
    rankLoading.value = false;
  }
}

async function fetchQuestionStats() {
  if (!selectedExamId.value) return;
  questionLoading.value = true;
  try {
    const { data } = await http.get<QuestionStatRow[]>('/api/exam/analytics/question-stats', {
      params: { examId: selectedExamId.value }
    });
    questionStats.value = data || [];
  } finally {
    questionLoading.value = false;
  }
}

async function fetchKpStats() {
  if (!selectedExamId.value) return;
  kpLoading.value = true;
  try {
    const { data } = await http.get<KpStatRow[]>('/api/exam/analytics/knowledge-point-stats', {
      params: { examId: selectedExamId.value }
    });
    kpStats.value = data || [];
  } finally {
    kpLoading.value = false;
  }
}

async function refreshAll() {
  if (!selectedExamId.value) return;
  await Promise.all([fetchOverview(), fetchRank(), fetchQuestionStats(), fetchKpStats()]);
}

async function onCourseChange() {
  await fetchExams();
  rankPage.value = 1;
  await refreshAll();
}

async function onExamChange() {
  rankPage.value = 1;
  await refreshAll();
}

async function exportRank() {
  if (!selectedExamId.value) return;
  const { data, headers } = await http.get('/api/exam/analytics/export-rank', {
    params: { examId: selectedExamId.value },
    responseType: 'blob'
  });
  const blob = new Blob([data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  const cd = String(headers?.['content-disposition'] || '');
  const m = cd.match(/filename\*=utf-8''([^;]+)/i);
  const fileName = m?.[1] ? decodeURIComponent(m[1]) : `exam-${selectedExamId.value}-rank.xlsx`;
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
  ElMessage.success('排名导出成功');
}

onMounted(async () => {
  await fetchCourses();
  const qCourse = Number(route.query.courseId);
  const qExam = Number(route.query.examId);
  if (Number.isFinite(qCourse) && qCourse > 0) {
    selectedCourseId.value = qCourse;
  } else {
    selectedCourseId.value = courseList.value[0]?.id;
  }
  await fetchExams();
  if (Number.isFinite(qExam) && qExam > 0) {
    selectedExamId.value = qExam;
  }
  await refreshAll();
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

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.label {
  color: #606266;
  font-size: 14px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.kpi-card {
  min-height: 90px;
}

.kpi-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
}

.overview-extra {
  margin-top: 12px;
}

.pass-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.main-tabs {
  margin-top: 12px;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.rate-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rate-cell > span {
  min-width: 66px;
  color: #606266;
  font-size: 13px;
}

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

