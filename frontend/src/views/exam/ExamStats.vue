<template>
  <el-card>
    <template #header>成绩统计</template>
    <div class="bar">
      <el-select v-model="courseId" placeholder="课程" filterable style="width: 220px" @change="onCourseChange">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="examId" placeholder="考试" filterable style="width: 260px" @change="loadStats">
        <el-option v-for="e in exams" :key="e.id" :label="e.title" :value="e.id" />
      </el-select>
    </div>

    <template v-if="overview">
      <el-descriptions title="概览" :column="2" border class="block">
        <el-descriptions-item label="考试 ID">{{ overview.examId }}</el-descriptions-item>
        <el-descriptions-item label="已交卷份数">{{ overview.submittedCount }}</el-descriptions-item>
        <el-descriptions-item label="平均分">{{ formatNum(overview.avgScore) }}</el-descriptions-item>
        <el-descriptions-item label="最高分">{{ formatNum(overview.maxScore) }}</el-descriptions-item>
        <el-descriptions-item label="最低分">{{ formatNum(overview.minScore) }}</el-descriptions-item>
        <el-descriptions-item label="及格线">{{ formatNum(overview.passScore) }}</el-descriptions-item>
        <el-descriptions-item label="及格人数">{{ overview.passCount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="成绩已发布">{{ overview.scorePublished ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="sub">排名</h4>
      <el-table :data="ranks" size="small">
        <el-table-column prop="rank" label="名次" width="72" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="totalScore" label="得分" width="88" />
        <el-table-column label="及格" width="72">
          <template #default="{ row }">{{ row.passed == null ? '—' : row.passed ? '是' : '否' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="rankPage"
        :page-size="10"
        :total="rankTotal"
        layout="total, prev, pager, next"
        class="pager"
        @current-change="loadRank"
      />
    </template>
    <el-empty v-else description="请选择课程与考试" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import { fetchTeacherExams, fetchExamOverview, fetchExamRank } from '@/api/examManage';
import { getLastCourseId, setLastCourseId } from '@/composables/useLastCourseId';

const courses = ref<CourseRow[]>([]);
const courseId = ref<number | null>(getLastCourseId());
const exams = ref<{ id: number; title: string }[]>([]);
const examId = ref<number | undefined>();

const overview = ref<Record<string, unknown> | null>(null);
const ranks = ref<Record<string, unknown>[]>([]);
const rankPage = ref(1);
const rankTotal = ref(0);

function formatNum(v: unknown) {
  if (v == null || v === '') return '—';
  const n = Number(v);
  return Number.isFinite(n) ? n.toFixed(2) : String(v);
}

async function loadCourses() {
  const { data } = await fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
  }
}

async function onCourseChange() {
  exams.value = [];
  examId.value = undefined;
  overview.value = null;
  if (courseId.value) {
    setLastCourseId(courseId.value);
    const { data } = await fetchTeacherExams(courseId.value, 1, 200);
    const rec = (data as { records?: { id: number; title: string }[] })?.records ?? [];
    exams.value = rec;
    if (rec.length) {
      examId.value = rec[0].id;
      await loadStats();
    }
  }
}

async function loadStats() {
  if (!examId.value) {
    overview.value = null;
    return;
  }
  const { data } = await fetchExamOverview(examId.value);
  overview.value = (data as Record<string, unknown>) ?? null;
  rankPage.value = 1;
  await loadRank();
}

async function loadRank() {
  if (!examId.value) return;
  const { data } = await fetchExamRank(examId.value, rankPage.value, 10);
  ranks.value = (data as { records?: Record<string, unknown>[] })?.records ?? [];
  rankTotal.value = (data as { total?: number })?.total ?? 0;
}

onMounted(async () => {
  await loadCourses();
  await onCourseChange();
});
</script>

<style scoped>
.bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
.block {
  margin-bottom: 16px;
}
.sub {
  margin: 16px 0 8px;
}
.pager {
  margin-top: 8px;
  justify-content: flex-end;
}
</style>
