<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <span>错题本</span>
      </template>
      <div class="row">
        <span>课程</span>
        <el-select v-model="courseId" placeholder="选择课程" filterable style="width: 320px" @change="load">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="rows" stripe style="margin-top: 16px">
        <el-table-column prop="questionId" label="题目 ID" width="100" />
        <el-table-column prop="type" label="题型" width="90" />
        <el-table-column prop="stem" label="题干" min-width="240" show-overflow-tooltip />
        <el-table-column prop="wrongCount" label="错误次数" width="100" />
        <el-table-column prop="lastWrongAt" label="最近出错" width="180" />
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as courseApi from '@/api/modules/course';
import * as wbApi from '@/api/modules/wrongbook';

const courses = ref<courseApi.Course[]>([]);
const courseId = ref<number | undefined>();
const loading = ref(false);
const rows = ref<wbApi.WrongRow[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

async function boot() {
  const data = await courseApi.fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
}

async function load() {
  if (!courseId.value) {
    rows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const data = await wbApi.fetchWrongPage(courseId.value, page.value, size.value);
    rows.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

onMounted(boot);
</script>

<style scoped>
.page {
  max-width: 1000px;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
