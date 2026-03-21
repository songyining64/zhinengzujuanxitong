<template>
  <el-card>
    <template #header>
      <span>可参加的考试</span>
    </template>
    <el-table v-loading="loading" :data="list" stripe style="width: 100%">
      <el-table-column prop="courseName" label="课程" min-width="120" />
      <el-table-column prop="title" label="考试名称" min-width="160" />
      <el-table-column label="考试时间" min-width="200">
        <template #default="{ row }">
          {{ formatRange(row.startTime, row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" :loading="startingId === row.id" @click="startExam(row)">
            进入考试
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && list.length === 0" description="暂无考试" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import http from '@/api/http';
import type { ExamStudentRow, ExamStartData } from '@/types/exam';

const router = useRouter();
const loading = ref(false);
const list = ref<ExamStudentRow[]>([]);
const startingId = ref<number | null>(null);

function formatRange(start: string, end: string) {
  return `${start?.replace('T', ' ').slice(0, 16)} ~ ${end?.replace('T', ' ').slice(0, 16)}`;
}

const fetchList = async () => {
  loading.value = true;
  try {
    const { data } = await http.get<{ records: ExamStudentRow[] }>('/api/exam/student', {
      params: { page: 1, size: 50 }
    });
    list.value = data?.records ?? [];
  } finally {
    loading.value = false;
  }
};

const startExam = async (row: ExamStudentRow) => {
  startingId.value = row.id;
  try {
    const { data } = await http.post<ExamStartData>(`/api/exam/${row.id}/start`);
    ElMessage.success('已开始考试，祝你好运');
    await router.push({
      name: 'ExamTake',
      params: { recordId: String(data.recordId) }
    });
  } catch {
    /* http 已提示 */
  } finally {
    startingId.value = null;
  }
};

onMounted(fetchList);
</script>
