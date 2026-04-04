<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <span>考试管理</span>
        <div class="row">
          <CoursePicker />
          <el-button type="primary" @click="openForm(null)">创建考试</el-button>
        </div>
      </div>
    </template>

    <el-alert v-if="!courseId" type="info" show-icon :closable="false" title="请先选择课程" />

    <template v-else>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="title" label="名称" min-width="140" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="paperId" label="试卷" width="80" />
        <el-table-column label="时间" min-width="200">
          <template #default="{ row }">
            {{ formatDt(row.startTime) }} ~ {{ formatDt(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长" width="72" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="primary" @click="doPublish(row)" v-if="row.status !== 'PUBLISHED'">发布</el-button>
            <el-button link type="warning" @click="doEnd(row)">结束</el-button>
            <el-button link type="success" @click="doPublishScore(row)">公布成绩</el-button>
            <el-button link @click="doUnpublishScore(row)">撤销成绩</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </template>

    <el-dialog v-model="dlg.visible" :title="dlg.id ? '编辑考试' : '创建考试'" width="560px" destroy-on-close>
      <el-form :model="dlg.form" label-width="120px">
        <el-form-item label="试卷 ID" required>
          <el-input-number v-model="dlg.form.paperId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="dlg.form.title" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="dlg.form.description" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="dlg.form.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="dlg.form.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="考试时长(分)" required>
          <el-input-number v-model="dlg.form.durationMinutes" :min="1" />
        </el-form-item>
        <el-form-item label="及格分">
          <el-input-number v-model="dlg.form.passScore" :min="0" />
        </el-form-item>
        <el-form-item label="题目乱序">
          <el-switch v-model="dlg.form.shuffleQuestions" />
        </el-form-item>
        <el-form-item label="选项乱序">
          <el-switch v-model="dlg.form.shuffleOptions" />
        </el-form-item>
        <el-form-item label="切屏上限">
          <el-input-number v-model="dlg.form.switchBlurLimit" :min="0" placeholder="0 不限制" />
        </el-form-item>
        <el-form-item label="公布成绩">
          <el-switch v-model="dlg.form.scorePublished" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import CoursePicker from '@/components/CoursePicker.vue';
import { useCourseContextStore } from '@/store/courseContext';
import {
  fetchTeacherExams,
  createExam,
  updateExam,
  publishExam,
  endExam,
  publishExamScore,
  unpublishExamScore
} from '@/api/modules/examTeacher';
import type { ExamCreateRequest, ExamEntity } from '@/types/examTeacher';

const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

const loading = ref(false);
const list = ref<ExamEntity[]>([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);

function formatDt(s: string) {
  return s?.replace('T', ' ').slice(0, 16) || '';
}

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchTeacherExams(courseId.value, page.value, size.value);
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

watch(courseId, () => {
  page.value = 1;
  list.value = [];
  total.value = 0;
  if (courseId.value) void load();
}, { immediate: true });

function defaultForm(): ExamCreateRequest {
  return {
    courseId: courseId.value!,
    paperId: 1,
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    durationMinutes: 90,
    passScore: 60,
    scorePublished: false,
    shuffleQuestions: true,
    shuffleOptions: true,
    switchBlurLimit: 0
  };
}

const dlg = reactive({
  visible: false,
  id: null as number | null,
  saving: false,
  form: defaultForm()
});

function openForm(row: ExamEntity | null) {
  if (!courseId.value) return;
  if (row) {
    dlg.id = row.id;
    dlg.form = {
      courseId: row.courseId,
      paperId: row.paperId,
      title: row.title,
      description: row.description || '',
      startTime: row.startTime,
      endTime: row.endTime,
      durationMinutes: row.durationMinutes,
      passScore: row.passScore != null ? Number(row.passScore) : undefined,
      scorePublished: !!row.scorePublished,
      shuffleQuestions: row.shuffleQuestions !== 0,
      shuffleOptions: row.shuffleOptions !== 0,
      switchBlurLimit: row.switchBlurLimit ?? 0
    };
  } else {
    dlg.id = null;
    dlg.form = defaultForm();
  }
  dlg.visible = true;
}

async function save() {
  if (!courseId.value) return;
  dlg.form.courseId = courseId.value;
  if (!dlg.form.title?.trim() || !dlg.form.paperId || !dlg.form.startTime || !dlg.form.endTime) {
    ElMessage.warning('请完善必填项');
    return;
  }
  dlg.saving = true;
  try {
    const body = {
      ...dlg.form,
      switchBlurLimit: dlg.form.switchBlurLimit || null
    };
    if (dlg.id) {
      await updateExam(dlg.id, body);
      ElMessage.success('已更新');
    } else {
      await createExam(body);
      ElMessage.success('已创建');
    }
    dlg.visible = false;
    await load();
  } finally {
    dlg.saving = false;
  }
}

async function doPublish(row: ExamEntity) {
  await publishExam(row.id);
  ElMessage.success('已发布');
  await load();
}

async function doEnd(row: ExamEntity) {
  await endExam(row.id);
  ElMessage.success('已结束');
  await load();
}

async function doPublishScore(row: ExamEntity) {
  await publishExamScore(row.id);
  ElMessage.success('成绩已公布');
  await load();
}

async function doUnpublishScore(row: ExamEntity) {
  await unpublishExamScore(row.id);
  ElMessage.success('已撤销成绩发布');
  await load();
}
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
