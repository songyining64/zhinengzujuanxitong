<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>考试管理</span>
        <el-button type="primary" :disabled="!courseId" @click="openCreate">新建考试</el-button>
      </div>
    </template>
    <div class="bar">
      <el-select v-model="courseId" placeholder="课程" filterable style="width: 260px" @change="onCourseChange">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="title" label="名称" min-width="160" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="考试时间" min-width="200">
        <template #default="{ row }">
          {{ fmt(row.startTime) }} ~ {{ fmt(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="时长(分)" width="96" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT'" type="primary" link @click="publish(row.id)">发布</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" type="warning" link @click="end(row.id)">结束</el-button>
          <el-button type="primary" link @click="toggleScore(row)">
            {{ row.scorePublished === 1 ? '撤回成绩' : '发布成绩' }}
          </el-button>
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dlg" :title="editId ? '编辑考试' : '新建考试'" width="560px" destroy-on-close @closed="resetForm">
      <el-form :model="form" label-width="112px">
        <el-form-item label="试卷" required>
          <el-select v-model="form.paperId" filterable placeholder="选择试卷" style="width: 100%">
            <el-option v-for="p in papers" :key="p.id" :label="`${p.id} ${p.title}`" :value="p.id" />
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
          <el-input-number v-model="form.durationMinutes" :min="1" />
        </el-form-item>
        <el-form-item label="及格分">
          <el-input-number v-model="form.passScore" :min="0" />
        </el-form-item>
        <el-form-item label="切屏上限">
          <el-input-number v-model="form.switchBlurLimit" :min="0" placeholder="0 不限制" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import { fetchPaperPage } from '@/api/modules/paper';
import {
  fetchTeacherExams,
  createExam,
  updateExam,
  publishExam,
  endExam,
  publishExamScore,
  unpublishExamScore,
  type ExamRow
} from '@/api/examManage';
import { getLastCourseId, setLastCourseId } from '@/composables/useLastCourseId';

const courses = ref<CourseRow[]>([]);
const courseId = ref<number | null>(getLastCourseId());
const papers = ref<{ id: number; title: string }[]>([]);
const loading = ref(false);
const rows = ref<ExamRow[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(20);

const dlg = ref(false);
const editId = ref<number | null>(null);
const saving = ref(false);
const form = reactive({
  paperId: undefined as number | undefined,
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  durationMinutes: 90,
  passScore: undefined as number | undefined,
  switchBlurLimit: undefined as number | undefined
});

function fmt(s: string) {
  return s?.replace('T', ' ').slice(0, 16) ?? '';
}

async function loadCourses() {
  const { data } = await fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
  }
}

async function loadPapers() {
  if (!courseId.value) {
    papers.value = [];
    return;
  }
  const pageData = await fetchPaperPage({ courseId: courseId.value, page: 1, size: 200 });
  const rec = pageData?.records ?? [];
  papers.value = rec.map((p) => ({ id: p.id, title: p.title }));
}

function onCourseChange() {
  if (courseId.value) setLastCourseId(courseId.value);
  void loadPapers();
  void load();
}

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchTeacherExams(courseId.value, page.value, size.value);
    rows.value = (data as { records?: ExamRow[] })?.records ?? [];
    total.value = (data as { total?: number })?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  editId.value = null;
  form.paperId = papers.value[0]?.id;
  form.title = '测验';
  form.description = '';
  const now = new Date();
  const end = new Date(now.getTime() + 86400000 * 7);
  form.startTime = toLocalIso(now);
  form.endTime = toLocalIso(end);
  form.durationMinutes = 90;
  form.passScore = 60;
  form.switchBlurLimit = 5;
}

function toLocalIso(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function openCreate() {
  resetForm();
  dlg.value = true;
}

function openEdit(row: ExamRow) {
  if (row.status !== 'DRAFT') {
    ElMessage.info('仅草稿可编辑，请使用发布/结束等操作');
    return;
  }
  editId.value = row.id;
  form.paperId = row.paperId;
  form.title = row.title;
  form.description = row.description || '';
  form.startTime = row.startTime.replace(' ', 'T').slice(0, 19);
  form.endTime = row.endTime.replace(' ', 'T').slice(0, 19);
  form.durationMinutes = row.durationMinutes;
  form.passScore = row.passScore != null ? Number(row.passScore) : undefined;
  form.switchBlurLimit = row.switchBlurLimit ?? undefined;
  dlg.value = true;
}

async function save() {
  if (!courseId.value || !form.paperId || !form.title.trim() || !form.startTime || !form.endTime) return;
  saving.value = true;
  try {
    const payload: Record<string, unknown> = {
      courseId: courseId.value,
      paperId: form.paperId,
      title: form.title,
      description: form.description || undefined,
      startTime: form.startTime,
      endTime: form.endTime,
      durationMinutes: form.durationMinutes,
      passScore: form.passScore,
      switchBlurLimit: form.switchBlurLimit === 0 ? 0 : form.switchBlurLimit || null,
      shuffleQuestions: true,
      shuffleOptions: true
    };
    if (editId.value) {
      await updateExam(editId.value, payload);
    } else {
      await createExam(payload);
    }
    dlg.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function publish(id: number) {
  await publishExam(id);
  await load();
}

async function end(id: number) {
  await endExam(id);
  await load();
}

async function toggleScore(row: ExamRow) {
  if (row.scorePublished === 1) {
    await unpublishExamScore(row.id);
  } else {
    await publishExamScore(row.id);
  }
  await load();
}

onMounted(async () => {
  await loadCourses();
  await loadPapers();
  await load();
});
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
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
