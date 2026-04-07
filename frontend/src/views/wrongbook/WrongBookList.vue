<template>
  <el-card>
    <template #header>错题本</template>
    <div class="bar">
      <el-select v-model="courseId" placeholder="课程" filterable style="width: 260px" @change="load">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" :disabled="!courseId" @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="questionId" label="试题ID" width="88" />
      <el-table-column prop="wrongCount" label="错次" width="72" />
      <el-table-column prop="lastWrongAt" label="最近错题时间" min-width="160" />
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import http from '@/api/http';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import { getLastCourseId, setLastCourseId } from '@/composables/useLastCourseId';

const courses = ref<CourseRow[]>([]);
const courseId = ref<number | null>(getLastCourseId());
const loading = ref(false);
const rows = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

async function loadCourses() {
  const { data } = await fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
  }
}

async function load() {
  if (!courseId.value) return;
  setLastCourseId(courseId.value);
  loading.value = true;
  try {
    const { data } = await http.get('/api/wrong-book', {
      params: { courseId: courseId.value, page: page.value, size: size.value }
    });
    rows.value = (data as { records?: Record<string, unknown>[] })?.records ?? [];
    total.value = (data as { total?: number })?.total ?? 0;
  } finally {
    loading.value = false;
  }
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
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
