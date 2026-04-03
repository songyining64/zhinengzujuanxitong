<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>考试管理</span>
          <el-button v-if="courseId" type="primary" @click="openCreate">新建考试</el-button>
        </div>
      </template>
      <div class="row">
        <span>课程</span>
        <el-select v-model="courseId" placeholder="选择课程" filterable style="width: 320px" @change="load">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="exams" stripe style="margin-top: 16px">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="title" label="名称" min-width="160" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="考试时间" min-width="200">
          <template #default="{ row }">
            {{ fmt(row.startTime) }} ~ {{ fmt(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长(分)" width="100" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="publish(row)">发布</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="end(row)">结束</el-button>
            <el-button v-if="!row.scorePublished" link type="success" @click="pubScore(row)">
              发布成绩
            </el-button>
            <el-button v-else link @click="unpubScore(row)">撤回成绩</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="新建考试" width="560px" destroy-on-close>
      <el-form :model="form" label-width="112px">
        <el-form-item label="试卷" required>
          <el-select v-model="form.paperId" filterable placeholder="先选择课程并等待试卷加载" style="width: 100%">
            <el-option v-for="p in papers" :key="p.id" :label="`${p.title} (#${p.id})`" :value="p.id" />
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
          <el-input-number v-model="form.passScore" :min="0" :step="0.5" />
        </el-form-item>
        <el-form-item label="切屏上限">
          <el-input-number v-model="form.switchBlurLimit" :min="0" placeholder="0 不限" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import * as courseApi from '@/api/modules/course';
import * as examApi from '@/api/modules/exam';
import * as paperApi from '@/api/modules/paper';

const courses = ref<courseApi.Course[]>([]);
const courseId = ref<number | undefined>();
const loading = ref(false);
const exams = ref<examApi.Exam[]>([]);
const papers = ref<paperApi.Paper[]>([]);

const visible = ref(false);
const saving = ref(false);
const form = reactive({
  paperId: undefined as number | undefined,
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  durationMinutes: 60,
  passScore: undefined as number | undefined,
  switchBlurLimit: undefined as number | undefined
});

function fmt(s: string) {
  return s?.replace('T', ' ').slice(0, 16) ?? '';
}

async function boot() {
  const data = await courseApi.fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
}

async function loadPapers() {
  if (!courseId.value) {
    papers.value = [];
    return;
  }
  const data = await paperApi.fetchPaperPage(courseId.value, 1, 100);
  papers.value = data?.records ?? [];
}

async function load() {
  if (!courseId.value) {
    exams.value = [];
    return;
  }
  loading.value = true;
  try {
    const data = await examApi.fetchTeacherExams(courseId.value, 1, 50);
    exams.value = data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

function localDateTime(d: Date) {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

function openCreate() {
  form.paperId = papers.value[0]?.id;
  form.title = '期中测验';
  form.description = '';
  const now = new Date();
  const end = new Date(now.getTime() + 86400000);
  form.startTime = localDateTime(now);
  form.endTime = localDateTime(end);
  form.durationMinutes = 60;
  form.passScore = 60;
  form.switchBlurLimit = 5;
  visible.value = true;
}

async function submit() {
  if (!courseId.value || !form.paperId || !form.title.trim() || !form.startTime || !form.endTime) {
    ElMessage.warning('请完善必填项');
    return;
  }
  saving.value = true;
  try {
    await examApi.createExam({
      courseId: courseId.value,
      paperId: form.paperId,
      title: form.title,
      description: form.description || undefined,
      startTime: form.startTime,
      endTime: form.endTime,
      durationMinutes: form.durationMinutes,
      passScore: form.passScore,
      switchBlurLimit: form.switchBlurLimit || null,
      shuffleQuestions: true,
      shuffleOptions: true,
      scorePublished: false
    });
    ElMessage.success('已创建');
    visible.value = false;
    await load();
  } catch {
    /* */
  } finally {
    saving.value = false;
  }
}

async function publish(row: examApi.Exam) {
  await examApi.publishExam(row.id);
  ElMessage.success('已发布');
  await load();
}

async function end(row: examApi.Exam) {
  await examApi.endExam(row.id);
  ElMessage.success('已结束');
  await load();
}

async function pubScore(row: examApi.Exam) {
  await examApi.publishScore(row.id);
  ElMessage.success('成绩已发布');
  await load();
}

async function unpubScore(row: examApi.Exam) {
  await examApi.unpublishScore(row.id);
  ElMessage.success('已撤回成绩发布');
  await load();
}

watch(courseId, async () => {
  await loadPapers();
  await load();
});

onMounted(boot);
</script>

<style scoped>
.page {
  max-width: 1200px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
