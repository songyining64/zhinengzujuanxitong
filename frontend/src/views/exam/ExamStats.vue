<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <span>成绩分析</span>
      </template>
      <div class="filters">
        <el-select v-model="courseId" placeholder="课程" filterable style="width: 260px" @change="onCourseChange">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="examId" placeholder="考试" filterable style="width: 320px" @change="loadOverview">
          <el-option v-for="e in examOptions" :key="e.id" :label="e.title" :value="e.id" />
        </el-select>
        <el-button type="primary" :disabled="!examId" @click="loadOverview">刷新</el-button>
      </div>

      <el-empty v-if="!overview && !loading" description="请选择考试" />

      <div v-else-if="overview" class="stats">
        <el-descriptions v-loading="loading" :column="2" border>
          <el-descriptions-item label="提交人数">{{ overview.submittedCount }}</el-descriptions-item>
          <el-descriptions-item label="答卷总数">{{ overview.totalRecords }}</el-descriptions-item>
          <el-descriptions-item label="平均分">{{ overview.avgScore ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="最高分 / 最低分">
            {{ overview.maxScore ?? '—' }} / {{ overview.minScore ?? '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="及格线">{{ overview.passScore ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="及格人数">{{ overview.passCount ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="成绩已发布">
            {{ overview.scorePublished ? '是' : '否' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as courseApi from '@/api/modules/course';
import * as examApi from '@/api/modules/exam';
import * as analyticsApi from '@/api/modules/analytics';

const courses = ref<courseApi.Course[]>([]);
const courseId = ref<number | undefined>();
const examOptions = ref<examApi.Exam[]>([]);
const examId = ref<number | undefined>();
const loading = ref(false);
const overview = ref<analyticsApi.ExamOverview | null>(null);

async function boot() {
  const data = await courseApi.fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
}

async function onCourseChange() {
  examId.value = undefined;
  overview.value = null;
  if (!courseId.value) {
    examOptions.value = [];
    return;
  }
  const data = await examApi.fetchTeacherExams(courseId.value, 1, 100);
  examOptions.value = data?.records ?? [];
}

async function loadOverview() {
  if (!examId.value) return;
  loading.value = true;
  try {
    overview.value = await analyticsApi.fetchOverview(examId.value);
  } finally {
    loading.value = false;
  }
}

onMounted(boot);
</script>

<style scoped>
.page {
  max-width: 900px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}
.stats {
  margin-top: 8px;
}
</style>
