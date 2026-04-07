<template>
  <el-card shadow="never">
    <template #header>
      <span>成绩分析</span>
    </template>

    <el-form :inline="true" class="bar">
      <el-form-item label="考试 ID">
        <el-input-number v-model="examId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="loadAll">加载</el-button>
      </el-form-item>
    </el-form>

    <div class="wrong-wrap">
      <div class="wrong-head">错题分布（卷内题号 · 题型）</div>
      <p v-if="!examId" class="wrong-empty muted">请先填写考试 ID。</p>
      <p v-else-if="!loadFinished" class="wrong-empty muted">点击「加载」后，将根据本场答题统计展示错题题号与题型。</p>
      <template v-else>
        <div v-if="usingDemoData" class="demo-hint">
          <el-tag type="warning" size="small" effect="light">演示数据</el-tag>
          <span>后端不可用时，以下为前端示例，便于查看版面效果。</span>
        </div>
        <el-table v-if="wrongQuestionRows.length" :data="wrongQuestionRows" size="small" stripe border class="wrong-table">
          <el-table-column prop="paperOrder" label="错题题号" width="120" align="center" />
          <el-table-column label="错题类型" min-width="160">
            <template #default="{ row }">{{ typeLabel(row.type) }}</template>
          </el-table-column>
        </el-table>
        <p v-else-if="qStats.length" class="wrong-empty">本场逐题正确率均为 100%，暂无错题明细。</p>
        <p v-else class="wrong-empty muted">暂无逐题数据（可能尚无人交卷或接口未返回）。请确认后端已启动并成功加载。</p>
      </template>
    </div>

    <template v-if="overview">
      <el-row :gutter="16" class="sum-cards">
        <el-col :xs="12" :sm="6">
          <el-statistic title="已交卷" :value="overview.submittedCount" />
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-statistic title="平均分" :value="overview.avgScore ?? 0" :precision="1" />
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-statistic title="最高分" :value="overview.maxScore ?? 0" :precision="1" />
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-statistic title="及格人数" :value="overview.passCount ?? 0" />
        </el-col>
      </el-row>

      <el-descriptions title="总览明细" :column="3" border class="block">
        <el-descriptions-item label="答卷/记录">{{ overview.submittedCount }} / {{ overview.totalRecords }}</el-descriptions-item>
        <el-descriptions-item label="最低分">{{ overview.minScore ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="及格线">{{ overview.passScore ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="成绩发布">{{ overview.scorePublished ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="tab" class="tabs">
        <el-tab-pane label="排名" name="rank">
          <div ref="chartRankRef" class="chart-box" />
          <el-table :data="ranks" size="small" stripe>
            <el-table-column prop="rank" label="名次" width="72" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="totalScore" label="得分" />
            <el-table-column label="及格" width="80">
              <template #default="{ row }">{{ row.passed == null ? '—' : row.passed ? '是' : '否' }}</template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="rankPage"
              layout="prev, pager, next"
              :total="rankTotal"
              :page-size="10"
              @current-change="loadRank"
            />
          </div>
        </el-tab-pane>
        <el-tab-pane label="逐题统计" name="q">
          <div ref="chartQRef" class="chart-box" />
          <el-table :data="qStats" size="small" stripe>
            <el-table-column prop="questionId" label="题目ID" width="88" />
            <el-table-column prop="type" label="题型" width="88" />
            <el-table-column prop="attemptCount" label="作答人次" />
            <el-table-column prop="correctCount" label="正确人次" />
            <el-table-column label="正确率">
              <template #default="{ row }">
                {{ row.correctRate != null ? (Number(row.correctRate) * 100).toFixed(1) + '%' : '—' }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="知识点" name="kp">
          <div ref="chartKpRef" class="chart-box" />
          <el-table :data="kpStats" size="small" stripe>
            <el-table-column prop="knowledgePointName" label="知识点" />
            <el-table-column prop="questionCount" label="题量" width="88" />
            <el-table-column label="平均正确率">
              <template #default="{ row }">
                {{ row.avgCorrectRate != null ? (Number(row.avgCorrectRate) * 100).toFixed(1) + '%' : '—' }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts';
import {
  fetchExamOverview,
  fetchExamRank,
  fetchQuestionStats,
  fetchKnowledgePointStats
} from '@/api/modules/analytics';
import { isDemoSession } from '@/constants/auth';
import type { ExamKnowledgeStatDTO, ExamOverviewDTO, ExamQuestionStatDTO, StudentRankDTO } from '@/types/models';

const examId = ref<number | undefined>(undefined);
const loading = ref(false);
/** 是否已点击过「加载」（避免错题区依赖 overview，否则接口失败时整块不显示） */
const loadFinished = ref(false);
/** 是否为前端注入的演示统计 */
const usingDemoData = ref(false);
const tab = ref('rank');

const overview = ref<ExamOverviewDTO | null>(null);
const ranks = ref<StudentRankDTO[]>([]);
const rankPage = ref(1);
const rankTotal = ref(0);
const qStats = ref<ExamQuestionStatDTO[]>([]);
const kpStats = ref<ExamKnowledgeStatDTO[]>([]);

/** 有作答且正确率未满 100% 视为错题，题号按试卷中题目顺序 */
const wrongQuestionRows = computed(() => {
  const rows: (ExamQuestionStatDTO & { paperOrder: number })[] = [];
  qStats.value.forEach((q, idx) => {
    if (q.attemptCount <= 0) return;
    const rate = q.correctRate != null ? Number(q.correctRate) : 0;
    if (rate < 1) {
      rows.push({ ...q, paperOrder: idx + 1 });
    }
  });
  return rows;
});

function typeLabel(type: string) {
  const m: Record<string, string> = {
    SINGLE: '单选',
    MULTIPLE: '多选',
    TRUE_FALSE: '判断',
    FILL: '填空',
    SHORT: '简答'
  };
  return m[type] || type || '—';
}

/** 无后端或请求失败时，演示登录下填充示例，保证错题表格可见 */
function applyDemoAnalytics(eid: number) {
  usingDemoData.value = true;
  overview.value = {
    examId: eid,
    submittedCount: 12,
    totalRecords: 15,
    avgScore: 78.5,
    maxScore: 98,
    minScore: 45,
    passScore: 60,
    scorePublished: true,
    passCount: 9
  };
  ranks.value = [
    { rank: 1, studentId: 1, username: 'stu_a', realName: '张三', totalScore: 92, passed: true },
    { rank: 2, studentId: 2, username: 'stu_b', realName: '李四', totalScore: 81, passed: true },
    { rank: 3, studentId: 3, username: 'stu_c', realName: '王五', totalScore: 58, passed: false }
  ];
  rankTotal.value = 3;
  qStats.value = [
    { questionId: 91001, type: 'SINGLE', attemptCount: 12, correctCount: 12, correctRate: 1 },
    { questionId: 91002, type: 'MULTIPLE', attemptCount: 12, correctCount: 8, correctRate: 0.667 },
    { questionId: 91003, type: 'TRUE_FALSE', attemptCount: 11, correctCount: 7, correctRate: 0.636 },
    { questionId: 91004, type: 'FILL', attemptCount: 10, correctCount: 6, correctRate: 0.6 },
    { questionId: 91005, type: 'SHORT', attemptCount: 9, correctCount: 9, correctRate: 1 }
  ];
  kpStats.value = [
    { knowledgePointId: 1, knowledgePointName: '函数与方程（示例）', questionCount: 3, avgCorrectRate: 0.72 },
    { knowledgePointId: 2, knowledgePointName: '立体几何（示例）', questionCount: 2, avgCorrectRate: 0.85 }
  ];
}

const chartRankRef = ref<HTMLDivElement | null>(null);
const chartQRef = ref<HTMLDivElement | null>(null);
const chartKpRef = ref<HTMLDivElement | null>(null);
let chartRank: echarts.ECharts | null = null;
let chartQ: echarts.ECharts | null = null;
let chartKp: echarts.ECharts | null = null;

function disposeCharts() {
  chartRank?.dispose();
  chartQ?.dispose();
  chartKp?.dispose();
  chartRank = chartQ = chartKp = null;
}

async function loadRank() {
  if (!examId.value) return;
  const { data } = await fetchExamRank(examId.value, rankPage.value, 10);
  ranks.value = data?.records ?? [];
  rankTotal.value = data?.total ?? 0;
}

function renderRankChart() {
  const el = chartRankRef.value;
  if (!el || !ranks.value.length) return;
  if (!chartRank) chartRank = echarts.init(el);
  chartRank.setOption({
    grid: { left: 48, right: 16, top: 24, bottom: 64 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ranks.value.map((r) => r.realName || r.username || String(r.studentId)),
      axisLabel: { rotate: 30 }
    },
    yAxis: { type: 'value', name: '得分' },
    series: [
      {
        name: '得分',
        type: 'bar',
        data: ranks.value.map((r) => (r.totalScore != null ? Number(r.totalScore) : 0)),
        itemStyle: { color: '#409eff' }
      }
    ]
  });
}

function renderQChart() {
  const el = chartQRef.value;
  if (!el || !qStats.value.length) return;
  if (!chartQ) chartQ = echarts.init(el);
  chartQ.setOption({
    grid: { left: 48, right: 16, top: 24, bottom: 48 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: qStats.value.map((q) => `Q${q.questionId}`) },
    yAxis: { type: 'value', max: 100, name: '正确率%' },
    series: [
      {
        type: 'line',
        smooth: true,
        data: qStats.value.map((q) =>
          q.correctRate != null ? Math.round(Number(q.correctRate) * 1000) / 10 : 0
        ),
        areaStyle: { opacity: 0.12 },
        itemStyle: { color: '#67c23a' }
      }
    ]
  });
}

function renderKpChart() {
  const el = chartKpRef.value;
  if (!el || !kpStats.value.length) return;
  if (!chartKp) chartKp = echarts.init(el);
  chartKp.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['36%', '68%'],
        data: kpStats.value.map((k) => ({
          name: k.knowledgePointName || `KP${k.knowledgePointId}`,
          value: k.questionCount
        }))
      }
    ]
  });
}

watch(tab, async () => {
  await nextTick();
  if (tab.value === 'rank') renderRankChart();
  if (tab.value === 'q') renderQChart();
  if (tab.value === 'kp') renderKpChart();
});

watch(ranks, async () => {
  await nextTick();
  if (tab.value === 'rank') renderRankChart();
});

watch(qStats, async () => {
  await nextTick();
  if (tab.value === 'q') renderQChart();
});

watch(kpStats, async () => {
  await nextTick();
  if (tab.value === 'kp') renderKpChart();
});

async function loadAll() {
  if (!examId.value) {
    overview.value = null;
    qStats.value = [];
    ranks.value = [];
    kpStats.value = [];
    rankTotal.value = 0;
    loadFinished.value = false;
    usingDemoData.value = false;
    disposeCharts();
    return;
  }
  loading.value = true;
  usingDemoData.value = false;
  try {
    disposeCharts();
    const { data: ov } = await fetchExamOverview(examId.value);
    overview.value = ov ?? null;
    rankPage.value = 1;
    await loadRank();
    const { data: qs } = await fetchQuestionStats(examId.value);
    qStats.value = qs ?? [];
    const { data: ks } = await fetchKnowledgePointStats(examId.value);
    kpStats.value = ks ?? [];
    await nextTick();
    renderRankChart();
    renderQChart();
    renderKpChart();
  } catch {
    overview.value = null;
    qStats.value = [];
    ranks.value = [];
    kpStats.value = [];
    rankTotal.value = 0;
    disposeCharts();
    if (isDemoSession()) {
      applyDemoAnalytics(examId.value);
      await nextTick();
      renderRankChart();
      renderQChart();
      renderKpChart();
    }
  } finally {
    loadFinished.value = true;
    loading.value = false;
  }
}

function onResize() {
  chartRank?.resize();
  chartQ?.resize();
  chartKp?.resize();
}

window.addEventListener('resize', onResize);
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  disposeCharts();
});
</script>

<style scoped>
.bar {
  margin-bottom: 16px;
}

.wrong-wrap {
  margin-bottom: 20px;
  padding: 12px 14px;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
}

.wrong-head {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 10px;
}

.wrong-table {
  background: #fff;
}

.wrong-empty {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.wrong-empty.muted {
  color: var(--el-text-color-placeholder);
}

.demo-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.sum-cards {
  margin-bottom: 16px;
}

.block {
  margin-bottom: 16px;
}

.tabs {
  margin-top: 8px;
}

.chart-box {
  width: 100%;
  height: 280px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
</style>
