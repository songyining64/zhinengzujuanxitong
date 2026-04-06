<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">考试管理</span>
        <el-button type="primary" :disabled="!selectedCourseId" @click="openCreate">+ 新增考试</el-button>
      </div>
    </template>

    <div class="toolbar">
      <span class="label">课程</span>
      <el-select
        v-model="selectedCourseId"
        placeholder="请选择课程"
        filterable
        clearable
        style="width: 280px"
        @change="onCourseChange"
      >
        <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
      </el-select>
      <el-button :disabled="!selectedCourseId" @click="fetchData">刷新</el-button>
    </div>

    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="考试名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="paperId" label="试卷ID" width="90" />
      <el-table-column label="考试时间" min-width="260">
        <template #default="{ row }">
          {{ formatRange(row.startTime, row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="成绩发布" width="100">
        <template #default="{ row }">
          <el-tag :type="row.scorePublished === 1 ? 'success' : 'info'" size="small">
            {{ row.scorePublished === 1 ? '已发布' : '未发布' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" min-width="400" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="goAnalytics(row)">成绩统计</el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="success"
            @click="onPublish(row)"
          >
            发布考试
          </el-button>
          <el-button
            v-if="row.status === 'PUBLISHED'"
            link
            type="warning"
            @click="onEnd(row)"
          >
            结束考试
          </el-button>
          <el-button
            v-if="row.scorePublished !== 1"
            link
            type="success"
            @click="onPublishScore(row)"
          >
            发布成绩
          </el-button>
          <el-button
            v-else
            link
            type="warning"
            @click="onUnpublishScore(row)"
          >
            撤回成绩
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </div>

    <el-dialog
      v-model="editVisible"
      :title="editingId ? '编辑考试' : '新增考试'"
      width="680px"
      destroy-on-close
      @closed="resetEdit"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="120px">
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="editForm.courseId" filterable placeholder="请选择课程" style="width: 100%" @change="onEditCourseChange">
            <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="试卷" prop="paperId">
          <el-select v-model="editForm.paperId" filterable placeholder="请选择试卷" style="width: 100%">
            <el-option v-for="p in paperOptions" :key="p.id" :label="paperLabel(p)" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试名称" prop="title">
          <el-input v-model="editForm.title" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="考试说明" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="editForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="editForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="考试时长(分钟)" prop="durationMinutes">
          <el-input-number v-model="editForm.durationMinutes" :min="1" :max="600" />
        </el-form-item>
        <el-form-item label="及格线">
          <el-input-number v-model="editForm.passScore" :min="0" :max="1000" :precision="1" :step="1" />
          <span class="hint">留空表示不设</span>
        </el-form-item>
        <el-form-item label="题目乱序">
          <el-switch v-model="editForm.shuffleQuestions" />
        </el-form-item>
        <el-form-item label="选项乱序">
          <el-switch v-model="editForm.shuffleOptions" />
        </el-form-item>
        <el-form-item label="切屏上限">
          <el-input-number v-model="editForm.switchBlurLimit" :min="0" :max="999" />
          <span class="hint">0 表示不限制</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '@/api/http';
import { useRouter } from 'vue-router';

interface CourseRow {
  id: number;
  name: string;
  code?: string;
}
interface CoursePage {
  records: CourseRow[];
  total: number;
}
interface PaperRow {
  id: number;
  title: string;
}
interface PaperPage {
  records: PaperRow[];
  total: number;
}
interface ExamRow {
  id: number;
  courseId: number;
  paperId: number;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  passScore?: number;
  scorePublished?: number;
  shuffleQuestions?: number;
  shuffleOptions?: number;
  switchBlurLimit?: number;
  status: string;
  createTime?: string;
}
interface ExamPage {
  records: ExamRow[];
  total: number;
}

const courseList = ref<CourseRow[]>([]);
const router = useRouter();
const selectedCourseId = ref<number | undefined>(undefined);
const paperOptions = ref<PaperRow[]>([]);

const list = ref<ExamRow[]>([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);

const editVisible = ref(false);
const editSubmitting = ref(false);
const editFormRef = ref<FormInstance>();
const editingId = ref<number | null>(null);
const editForm = reactive({
  courseId: undefined as number | undefined,
  paperId: undefined as number | undefined,
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  durationMinutes: 60,
  passScore: undefined as number | undefined,
  scorePublished: false,
  shuffleQuestions: true,
  shuffleOptions: true,
  switchBlurLimit: 0
});

const editRules: FormRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }],
  title: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  durationMinutes: [{ required: true, message: '请输入考试时长', trigger: 'change' }]
};

function courseLabel(c: CourseRow) {
  return c.code ? `${c.name}（${c.code}）` : c.name;
}
function paperLabel(p: PaperRow) {
  return `${p.id} - ${p.title}`;
}
function formatRange(start?: string, end?: string) {
  const a = start?.replace('T', ' ').slice(0, 16) || '-';
  const b = end?.replace('T', ' ').slice(0, 16) || '-';
  return `${a} ~ ${b}`;
}
function statusLabel(s?: string) {
  if (s === 'DRAFT') return '草稿';
  if (s === 'PUBLISHED') return '进行中';
  if (s === 'ENDED') return '已结束';
  return s || '-';
}
function statusTagType(s?: string) {
  if (s === 'DRAFT') return 'info';
  if (s === 'PUBLISHED') return 'success';
  if (s === 'ENDED') return 'warning';
  return '';
}

async function fetchCourses() {
  const { data } = await http.get<CoursePage>('/api/course', { params: { page: 1, size: 200 } });
  courseList.value = data?.records ?? [];
  if (!selectedCourseId.value && courseList.value.length) {
    selectedCourseId.value = courseList.value[0].id;
  }
}

async function fetchPaperOptions(courseId?: number) {
  if (!courseId) {
    paperOptions.value = [];
    return;
  }
  const { data } = await http.get<PaperPage>('/api/paper', {
    params: { courseId, page: 1, size: 200 }
  });
  paperOptions.value = data?.records ?? [];
}

const fetchData = async () => {
  if (!selectedCourseId.value) {
    list.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const { data } = await http.get<ExamPage>('/api/exam/teacher', {
      params: { courseId: selectedCourseId.value, page: page.value, size: size.value }
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

async function onCourseChange() {
  page.value = 1;
  await fetchData();
}

async function onEditCourseChange(v: number | undefined) {
  editForm.paperId = undefined;
  await fetchPaperOptions(v);
}

async function openCreate() {
  editingId.value = null;
  editForm.courseId = selectedCourseId.value;
  editForm.paperId = undefined;
  editForm.title = '';
  editForm.description = '';
  editForm.startTime = '';
  editForm.endTime = '';
  editForm.durationMinutes = 60;
  editForm.passScore = undefined;
  editForm.scorePublished = false;
  editForm.shuffleQuestions = true;
  editForm.shuffleOptions = true;
  editForm.switchBlurLimit = 0;
  await fetchPaperOptions(editForm.courseId);
  editFormRef.value?.clearValidate();
  editVisible.value = true;
}

async function openEdit(row: ExamRow) {
  editingId.value = row.id;
  editForm.courseId = row.courseId;
  await fetchPaperOptions(editForm.courseId);
  editForm.paperId = row.paperId;
  editForm.title = row.title;
  editForm.description = row.description ?? '';
  editForm.startTime = row.startTime;
  editForm.endTime = row.endTime;
  editForm.durationMinutes = row.durationMinutes;
  editForm.passScore = row.passScore ?? undefined;
  editForm.scorePublished = row.scorePublished === 1;
  editForm.shuffleQuestions = row.shuffleQuestions !== 0;
  editForm.shuffleOptions = row.shuffleOptions !== 0;
  editForm.switchBlurLimit = row.switchBlurLimit ?? 0;
  editFormRef.value?.clearValidate();
  editVisible.value = true;
}

function resetEdit() {
  editingId.value = null;
  editFormRef.value?.resetFields();
}

async function submitEdit() {
  const el = editFormRef.value;
  if (!el) return;
  await el.validate(async (valid) => {
    if (!valid) return;
    editSubmitting.value = true;
    try {
      const payload = {
        courseId: editForm.courseId,
        paperId: editForm.paperId,
        title: editForm.title.trim(),
        description: editForm.description.trim() || undefined,
        startTime: editForm.startTime,
        endTime: editForm.endTime,
        durationMinutes: editForm.durationMinutes,
        passScore: editForm.passScore ?? undefined,
        scorePublished: !!editForm.scorePublished,
        shuffleQuestions: !!editForm.shuffleQuestions,
        shuffleOptions: !!editForm.shuffleOptions,
        switchBlurLimit: editForm.switchBlurLimit ?? 0
      };
      if (editingId.value == null) {
        await http.post('/api/exam', payload);
        ElMessage.success('创建成功');
      } else {
        await http.put(`/api/exam/${editingId.value}`, payload);
        ElMessage.success('保存成功');
      }
      editVisible.value = false;
      await fetchData();
    } finally {
      editSubmitting.value = false;
    }
  });
}

async function onPublish(row: ExamRow) {
  await http.post(`/api/exam/${row.id}/publish`);
  ElMessage.success('考试已发布');
  await fetchData();
}
async function onEnd(row: ExamRow) {
  try {
    await ElMessageBox.confirm('确定提前结束该考试吗？', '结束考试', { type: 'warning' });
  } catch {
    return;
  }
  await http.post(`/api/exam/${row.id}/end`);
  ElMessage.success('考试已结束');
  await fetchData();
}
async function onPublishScore(row: ExamRow) {
  await http.post(`/api/exam/${row.id}/publish-score`);
  ElMessage.success('成绩已发布');
  await fetchData();
}
async function onUnpublishScore(row: ExamRow) {
  await http.post(`/api/exam/${row.id}/unpublish-score`);
  ElMessage.success('成绩已撤回');
  await fetchData();
}

function goAnalytics(row: ExamRow) {
  void router.push({
    name: 'ExamAnalytics',
    query: {
      examId: String(row.id),
      courseId: String(row.courseId)
    }
  });
}

onMounted(async () => {
  await fetchCourses();
  await fetchData();
});
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.label {
  color: #606266;
  font-size: 14px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>

