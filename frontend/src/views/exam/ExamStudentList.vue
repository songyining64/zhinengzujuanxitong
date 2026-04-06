<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>学生考试</span>
        <el-button @click="fetchList">刷新</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="按考试名称搜索"
        clearable
        style="width: 280px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-button type="primary" @click="onSearch">搜索</el-button>
    </div>

    <el-table v-loading="loading" :data="list" stripe style="width: 100%">
      <el-table-column prop="courseName" label="课程" min-width="120" />
      <el-table-column prop="title" label="考试名称" min-width="160" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(examStage(row))" size="small">
            {{ statusLabel(examStage(row)) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="考试时间" min-width="200">
        <template #default="{ row }">
          {{ formatRange(row.startTime, row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            :disabled="examStage(row) !== 'ONGOING'"
            :loading="startingId === row.id"
            @click="startExam(row)"
          >
            {{ actionText(row) }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && list.length === 0" description="暂无考试" />
    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchList"
        @size-change="onSizeChange"
      />
    </div>
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
const page = ref(1);
const size = ref(10);
const total = ref(0);

function formatRange(start: string, end: string) {
  return `${start?.replace('T', ' ').slice(0, 16)} ~ ${end?.replace('T', ' ').slice(0, 16)}`;
}

type Stage = 'NOT_STARTED' | 'ONGOING' | 'ENDED';

function examStage(row: ExamStudentRow): Stage {
  const now = Date.now();
  const startAt = new Date(row.startTime).getTime();
  const endAt = new Date(row.endTime).getTime();
  if (Number.isFinite(startAt) && now < startAt) return 'NOT_STARTED';
  if (Number.isFinite(endAt) && now > endAt) return 'ENDED';
  return 'ONGOING';
}

function statusLabel(stage: Stage) {
  if (stage === 'NOT_STARTED') return '未开始';
  if (stage === 'ONGOING') return '进行中';
  return '已结束';
}

function statusTagType(stage: Stage) {
  if (stage === 'NOT_STARTED') return 'info';
  if (stage === 'ONGOING') return 'success';
  return 'warning';
}

function actionText(row: ExamStudentRow) {
  const stage = examStage(row);
  if (stage === 'NOT_STARTED') return '未到开考时间';
  if (stage === 'ENDED') return '考试已结束';
  return '进入考试';
}

const fetchList = async () => {
  loading.value = true;
  try {
    const { data } = await http.get<{ records: ExamStudentRow[]; total: number }>('/api/exam/student', {
      params: { page: page.value, size: size.value, keyword: keyword.value || undefined }
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

function onSearch() {
  page.value = 1;
  void fetchList();
}

function onSizeChange() {
  page.value = 1;
  void fetchList();
}

const startExam = async (row: ExamStudentRow) => {
  if (examStage(row) !== 'ONGOING') return;
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
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
