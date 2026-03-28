<template>
  <el-card>
    <div class="toolbar">
      <span class="label">课程</span>
      <el-select v-model="courseId" filterable placeholder="选择课程" style="width: 260px" @change="fetchData">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </div>

    <el-empty v-if="!courseId" description="请选择课程" />
    <el-table v-else v-loading="loading" :data="list" stripe>
      <el-table-column prop="questionId" label="题目ID" width="88" />
      <el-table-column prop="type" label="题型" width="90" />
      <el-table-column prop="wrongCount" label="错误次数" width="96" />
      <el-table-column prop="lastWrongAt" label="最近错题时间" width="180" />
      <el-table-column label="题干摘要" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.stem?.slice(0, 100) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="courseId"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="fetchData"
      @size-change="fetchData"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import http from '@/api/http';

interface CourseBrief {
  id: number;
  name: string;
}

interface WrongRow {
  questionId: number;
  type: string;
  wrongCount: number;
  lastWrongAt: string;
  stem: string;
}

const courses = ref<CourseBrief[]>([]);
const courseId = ref<number | undefined>(undefined);
const page = ref(1);
const size = ref(10);
const total = ref(0);
const list = ref<WrongRow[]>([]);
const loading = ref(false);

const loadCourses = async () => {
  const { data } = await http.get<{ records: CourseBrief[] }>('/api/course', { params: { page: 1, size: 500 } });
  courses.value = data?.records ?? [];
};

const fetchData = async () => {
  if (!courseId.value) {
    list.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const { data } = await http.get<{ records: WrongRow[]; total: number }>('/api/wrong-book', {
      params: { courseId: courseId.value, page: page.value, size: size.value }
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await loadCourses();
  if (courses.value.length) {
    courseId.value = courses.value[0].id;
    await fetchData();
  }
});
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.label {
  margin-right: 8px;
  color: #606266;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
