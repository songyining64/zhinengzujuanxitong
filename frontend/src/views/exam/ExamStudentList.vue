<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>可参加的考试</span>
        <el-input
          v-model="keyword"
          placeholder="搜索考试名称"
          clearable
          style="width: 220px"
          @keyup.enter="fetchList"
        />
        <el-button type="primary" @click="fetchList">搜索</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="list" stripe style="width: 100%">
      <el-table-column prop="courseName" label="课程" min-width="120" />
      <el-table-column prop="title" label="考试名称" min-width="160" />
      <el-table-column label="阶段" width="100">
        <template #default="{ row }">
          <el-tag :type="phaseType(row)" size="small">{{ phaseLabel(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="考试时间" min-width="200">
        <template #default="{ row }">
          {{ formatRange(row.startTime, row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            :disabled="!inWindow(row)"
            :loading="startingId === row.id"
            @click="startExam(row)"
          >
            进入考试
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && list.length === 0" description="暂无考试" />
    <p v-if="list.length" class="hint">仅在「进行中」且考试已发布时可进入；是否已选课、是否在时间窗口内由后端校验。</p>
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
const keyword = ref('');

function formatRange(start: string, end: string) {
  return `${start?.replace('T', ' ').slice(0, 16)} ~ ${end?.replace('T', ' ').slice(0, 16)}`;
}

function parseDt(s: string) {
  return new Date(s.replace(' ', 'T'));
}

function phaseLabel(row: ExamStudentRow) {
  const now = Date.now();
  const t0 = parseDt(row.startTime).getTime();
  const t1 = parseDt(row.endTime).getTime();
  if (now < t0) return '未开始';
  if (now > t1) return '已结束';
  return '进行中';
}

function phaseType(row: ExamStudentRow) {
  const p = phaseLabel(row);
  if (p === '未开始') return 'info';
  if (p === '已结束') return 'info';
  return 'success';
}

function inWindow(row: ExamStudentRow) {
  const now = Date.now();
  const t0 = parseDt(row.startTime).getTime();
  const t1 = parseDt(row.endTime).getTime();
  return now >= t0 && now <= t1 && row.status === 'PUBLISHED';
}

const fetchList = async () => {
  loading.value = true;
  try {
    const { data } = await http.get<{ records: ExamStudentRow[] }>('/api/exam/student', {
      params: { page: 1, size: 50, keyword: keyword.value || undefined }
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

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.hint {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
