<template>
  <el-card>
    <div class="toolbar">
      <span class="label">课程</span>
      <el-select v-model="courseId" filterable style="width: 220px" @change="onCourseChange">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button v-if="courseId" type="primary" style="margin-left: auto" @click="openCreate">创建考试</el-button>
    </div>

    <el-empty v-if="!courseId" description="请选择课程" />
    <el-table v-else v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="68" />
      <el-table-column prop="title" label="名称" min-width="140" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="起止时间" min-width="200">
        <template #default="{ row }">
          {{ fmt(row.startTime) }} ~ {{ fmt(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="时长(分)" width="88" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="publish(row)">发布</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="endExam(row)">结束</el-button>
          <el-button
            v-if="row.status === 'ENDED' && !row.scorePublished"
            link
            type="primary"
            @click="publishScore(row)"
          >
            发布成绩
          </el-button>
          <el-button v-if="row.scorePublished" link @click="unpublishScore(row)">撤销成绩</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="courseId"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="fetchExams"
      @size-change="fetchExams"
    />

    <el-dialog v-model="formVisible" title="创建考试" width="520px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-form-item label="试卷" required>
          <el-select v-model="form.paperId" filterable placeholder="选择试卷" style="width: 100%">
            <el-option v-for="p in papers" :key="p.id" :label="`${p.id} - ${p.title}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试名称" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="考试时长(分)" required>
          <el-input-number v-model="form.durationMinutes" :min="1" :max="600" />
        </el-form-item>
        <el-form-item label="及格分">
          <el-input-number v-model="form.passScore" :min="0" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveExam">创建</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import http from '@/api/http';

interface CourseBrief {
  id: number;
  name: string;
}

interface PaperBrief {
  id: number;
  title: string;
}

interface ExamRow {
  id: number;
  title: string;
  status: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  scorePublished?: number;
}

const courses = ref<CourseBrief[]>([]);
const courseId = ref<number | undefined>(undefined);
const papers = ref<PaperBrief[]>([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);
const list = ref<ExamRow[]>([]);
const loading = ref(false);

const formVisible = ref(false);
const saving = ref(false);
const form = reactive({
  paperId: undefined as number | undefined,
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  durationMinutes: 60,
  passScore: undefined as number | undefined
});

const fmt = (t: string) => (t ? t.replace('T', ' ').slice(0, 16) : '');

const loadCourses = async () => {
  const { data } = await http.get<{ records: CourseBrief[] }>('/api/course', { params: { page: 1, size: 500 } });
  courses.value = data?.records ?? [];
};

const loadPapers = async () => {
  if (!courseId.value) {
    papers.value = [];
    return;
  }
  const { data } = await http.get<{ records: PaperBrief[] }>('/api/paper', {
    params: { courseId: courseId.value, page: 1, size: 200 }
  });
  papers.value = data?.records ?? [];
};

const fetchExams = async () => {
  if (!courseId.value) {
    list.value = [];
    return;
  }
  loading.value = true;
  try {
    const { data } = await http.get<{ records: ExamRow[]; total: number }>('/api/exam/teacher', {
      params: { courseId: courseId.value, page: page.value, size: size.value }
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

const onCourseChange = async () => {
  await loadPapers();
  await fetchExams();
};

const openCreate = () => {
  form.paperId = papers.value[0]?.id;
  form.title = '';
  form.description = '';
  form.durationMinutes = 60;
  form.passScore = undefined;
  const now = new Date();
  const later = new Date(now.getTime() + 86400000);
  form.startTime = toLocalIso(now);
  form.endTime = toLocalIso(later);
  formVisible.value = true;
};

function toLocalIso(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
}

const saveExam = async () => {
  if (!courseId.value || !form.paperId || !form.title.trim() || !form.startTime || !form.endTime) return;
  saving.value = true;
  try {
    await http.post('/api/exam', {
      courseId: courseId.value,
      paperId: form.paperId,
      title: form.title,
      description: form.description || undefined,
      startTime: form.startTime,
      endTime: form.endTime,
      durationMinutes: form.durationMinutes,
      passScore: form.passScore
    });
    formVisible.value = false;
    await fetchExams();
  } finally {
    saving.value = false;
  }
};

const publish = async (row: ExamRow) => {
  await http.post(`/api/exam/${row.id}/publish`);
  await fetchExams();
};

const endExam = async (row: ExamRow) => {
  await ElMessageBox.confirm('确定结束该考试？', '提示', { type: 'warning' });
  await http.post(`/api/exam/${row.id}/end`);
  await fetchExams();
};

const publishScore = async (row: ExamRow) => {
  await http.post(`/api/exam/${row.id}/publish-score`);
  await fetchExams();
};

const unpublishScore = async (row: ExamRow) => {
  await http.post(`/api/exam/${row.id}/unpublish-score`);
  await fetchExams();
};

onMounted(async () => {
  await loadCourses();
  if (courses.value.length) {
    courseId.value = courses.value[0].id;
    await onCourseChange();
  }
});
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.label {
  color: #606266;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
