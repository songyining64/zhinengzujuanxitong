<template>
  <el-card>
    <template #header>试卷浏览</template>
    <div class="bar">
      <el-select v-model="courseId" placeholder="课程" filterable style="width: 260px" @change="onCourseChange">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" :disabled="!courseId" @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="mode" label="模式" width="88" />
      <el-table-column prop="totalScore" label="总分" width="88" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />

    <el-drawer v-model="detailVisible" title="试卷详情" size="640px" destroy-on-close>
      <template v-if="detail">
        <p><strong>标题：</strong>{{ detail.paper?.title }}</p>
        <p><strong>模式：</strong>{{ detail.paper?.mode }} &nbsp; <strong>总分：</strong>{{ detail.paper?.totalScore }}</p>
        <el-table :data="detail.questions" size="small" class="mt">
          <el-table-column prop="questionOrder" label="#" width="50" />
          <el-table-column prop="type" label="题型" width="100" />
          <el-table-column prop="score" label="分值" width="72" />
          <el-table-column prop="stem" label="题干" min-width="200" show-overflow-tooltip />
        </el-table>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import { fetchPaperPage, getPaperDetail } from '@/api/modules/paper';
import { getLastCourseId, setLastCourseId } from '@/composables/useLastCourseId';

const courses = ref<CourseRow[]>([]);
const courseId = ref<number | null>(getLastCourseId());
const loading = ref(false);
const rows = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

const detailVisible = ref(false);
const detail = ref<{
  paper?: { title?: string; mode?: string; totalScore?: number };
  questions?: { questionOrder?: number; type?: string; score?: number; stem?: string }[];
} | null>(null);

async function loadCourses() {
  const { data } = await fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
  }
}

function onCourseChange() {
  if (courseId.value) setLastCourseId(courseId.value);
  void load();
}

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchPaperPage({ courseId: courseId.value, page: page.value, size: size.value });
    rows.value = (data as { records?: Record<string, unknown>[] })?.records ?? [];
    total.value = (data as { total?: number })?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

async function openDetail(id: number) {
  detailVisible.value = true;
  detail.value = null;
  const { data } = await getPaperDetail(id);
  detail.value = data as typeof detail.value;
}

onMounted(async () => {
  await loadCourses();
  await load();
});
</script>

<style scoped>
.bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.mt {
  margin-top: 12px;
}
</style>
