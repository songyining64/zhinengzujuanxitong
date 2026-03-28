<template>
  <el-card>
    <div class="toolbar">
      <span class="label">课程</span>
      <el-select
        v-model="courseId"
        filterable
        placeholder="课程"
        style="width: 200px"
        @change="onCourseChange"
      >
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <span class="label">知识点</span>
      <el-select
        v-model="knowledgePointId"
        clearable
        placeholder="全部"
        style="width: 180px"
        @change="fetchData"
      >
        <el-option v-for="k in knowledgePoints" :key="k.id" :label="k.name" :value="k.id" />
      </el-select>
      <el-input v-model="keyword" placeholder="题干关键词" clearable style="width: 160px" @keyup.enter="fetchData" />
      <el-button type="primary" @click="fetchData">搜索</el-button>
      <div v-if="canManage && courseId" class="spacer" />
      <template v-if="canManage && courseId">
        <el-button type="success" @click="openCreate">新建试题</el-button>
        <el-button @click="downloadTemplate">下载导入模板</el-button>
        <el-upload
          :show-file-list="false"
          accept=".xlsx,.xls"
          :http-request="doImport"
        >
          <el-button :loading="importing">Excel 导入</el-button>
        </el-upload>
        <el-button @click="exportExcel">导出 Excel</el-button>
      </template>
    </div>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="68" />
      <el-table-column prop="type" label="题型" width="100" />
      <el-table-column label="题干" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.stem?.slice(0, 80) }}{{ row.stem?.length > 80 ? '…' : '' }}</template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="72" />
      <el-table-column prop="reviewStatus" label="审核" width="100" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <template v-if="canManage">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
            <el-button
              v-if="row.reviewStatus === 'DRAFT' || row.reviewStatus === 'REJECTED'"
              link
              type="warning"
              @click="submitReview(row)"
            >
              提交审核
            </el-button>
            <template v-if="isAdmin">
              <el-button v-if="row.reviewStatus === 'PENDING'" link type="success" @click="approve(row)">通过</el-button>
              <el-button v-if="row.reviewStatus === 'PENDING'" link type="danger" @click="reject(row)">驳回</el-button>
            </template>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="fetchData"
      @size-change="fetchData"
    />

    <el-dialog v-model="formVisible" :title="editingId ? '编辑试题' : '新建试题'" width="640px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="知识点" required>
          <el-select v-model="form.knowledgePointId" filterable style="width: 100%">
            <el-option v-for="k in knowledgePoints" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" required>
          <el-select v-model="form.type" style="width: 200px">
            <el-option label="单选 SINGLE" value="SINGLE" />
            <el-option label="多选 MULTIPLE" value="MULTIPLE" />
            <el-option label="判断 TRUE_FALSE" value="TRUE_FALSE" />
            <el-option label="填空 FILL" value="FILL" />
            <el-option label="简答 SHORT" value="SHORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="form.stem" type="textarea" rows="4" />
        </el-form-item>
        <el-form-item label="选项 JSON">
          <el-input v-model="form.optionsJson" type="textarea" rows="3" placeholder='如 [{"key":"A","text":"选项A"}]' />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="form.answer" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="默认分值">
          <el-input-number v-model="form.scoreDefault" :min="0" :step="0.5" />
        </el-form-item>
        <el-form-item label="难度 1-3">
          <el-input-number v-model="form.difficulty" :min="1" :max="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="试题详情" size="480px">
      <pre v-if="detail" class="json-block">{{ detailText }}</pre>
    </el-drawer>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import type { UploadRequestOptions } from 'element-plus';
import http from '@/api/http';

interface CourseBrief {
  id: number;
  name: string;
}

interface KpRow {
  id: number;
  name: string;
}

interface QRow {
  id: number;
  type: string;
  stem: string;
  difficulty?: number;
  reviewStatus?: string;
}

const role = () => localStorage.getItem('role') || '';
const canManage = computed(() => role() === 'ADMIN' || role() === 'TEACHER');
const isAdmin = computed(() => role() === 'ADMIN');

const courses = ref<CourseBrief[]>([]);
const courseId = ref<number | undefined>(undefined);
const knowledgePoints = ref<KpRow[]>([]);
const knowledgePointId = ref<number | undefined>(undefined);
const keyword = ref('');
const page = ref(1);
const size = ref(10);
const total = ref(0);
const list = ref<QRow[]>([]);
const loading = ref(false);
const importing = ref(false);

const formVisible = ref(false);
const editingId = ref<number | null>(null);
const form = reactive({
  knowledgePointId: undefined as number | undefined,
  type: 'SINGLE',
  stem: '',
  optionsJson: '',
  answer: '',
  analysis: '',
  scoreDefault: undefined as number | undefined,
  difficulty: 2
});
const saving = ref(false);

const detailVisible = ref(false);
const detail = ref<Record<string, unknown> | null>(null);
const detailText = computed(() => (detail.value ? JSON.stringify(detail.value, null, 2) : ''));

const loadCourses = async () => {
  const { data } = await http.get<{ records: CourseBrief[] }>('/api/course', { params: { page: 1, size: 500 } });
  courses.value = data?.records ?? [];
};

const loadKnowledge = async () => {
  if (!courseId.value) {
    knowledgePoints.value = [];
    return;
  }
  const { data } = await http.get<KpRow[]>('/api/knowledge-point', { params: { courseId: courseId.value } });
  knowledgePoints.value = data ?? [];
};

const onCourseChange = async () => {
  knowledgePointId.value = undefined;
  await loadKnowledge();
  await fetchData();
};

const fetchData = async () => {
  if (!courseId.value) {
    list.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const { data } = await http.get<{ records: QRow[]; total: number }>('/api/question', {
      params: {
        courseId: courseId.value,
        knowledgePointId: knowledgePointId.value || undefined,
        keyword: keyword.value || undefined,
        page: page.value,
        size: size.value
      }
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  if (!courseId.value) return;
  editingId.value = null;
  form.knowledgePointId = knowledgePoints.value[0]?.id;
  form.type = 'SINGLE';
  form.stem = '';
  form.optionsJson = '';
  form.answer = '';
  form.analysis = '';
  form.scoreDefault = undefined;
  form.difficulty = 2;
  formVisible.value = true;
};

const openEdit = async (row: QRow) => {
  const { data } = await http.get<Record<string, unknown>>(`/api/question/${row.id}`);
  editingId.value = row.id;
  form.knowledgePointId = (data.knowledgePointId as number) || undefined;
  form.type = (data.type as string) || 'SINGLE';
  form.stem = (data.stem as string) || '';
  form.optionsJson = (data.optionsJson as string) || '';
  form.answer = (data.answer as string) || '';
  form.analysis = (data.analysis as string) || '';
  form.scoreDefault = data.scoreDefault != null ? Number(data.scoreDefault) : undefined;
  form.difficulty = (data.difficulty as number) || 2;
  formVisible.value = true;
};

const save = async () => {
  if (!courseId.value || !form.knowledgePointId || !form.stem.trim() || !form.answer.trim()) return;
  saving.value = true;
  try {
    const body = {
      knowledgePointId: form.knowledgePointId,
      type: form.type,
      stem: form.stem,
      optionsJson: form.optionsJson || undefined,
      answer: form.answer,
      analysis: form.analysis || undefined,
      scoreDefault: form.scoreDefault,
      difficulty: form.difficulty
    };
    if (editingId.value) {
      await http.put(`/api/question/${editingId.value}`, body);
    } else {
      await http.post('/api/question', { courseId: courseId.value, ...body, reviewStatus: 'DRAFT' });
    }
    formVisible.value = false;
    await fetchData();
  } finally {
    saving.value = false;
  }
};

const remove = async (row: QRow) => {
  await ElMessageBox.confirm('确定删除该试题？', '提示', { type: 'warning' });
  await http.delete(`/api/question/${row.id}`);
  await fetchData();
};

const submitReview = async (row: QRow) => {
  await http.post(`/api/question/${row.id}/submit-review`);
  await fetchData();
};

const approve = async (row: QRow) => {
  await http.post(`/api/question/${row.id}/approve`);
  await fetchData();
};

const reject = async (row: QRow) => {
  await ElMessageBox.confirm('确定驳回该试题？', '驳回', { type: 'warning' });
  await http.post(`/api/question/${row.id}/reject`);
  await fetchData();
};

const openDetail = async (row: QRow) => {
  const { data } = await http.get(`/api/question/${row.id}`);
  detail.value = data as Record<string, unknown>;
  detailVisible.value = true;
};

const authHeader = () => ({ Authorization: `Bearer ${localStorage.getItem('access_token') || ''}` });

const downloadBlob = (blob: Blob, filename: string) => {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
};

const downloadTemplate = async () => {
  const res = await fetch('/api/question/import/template', { headers: authHeader() });
  const blob = await res.blob();
  downloadBlob(blob, 'question-import-template.xlsx');
};

const exportExcel = async () => {
  if (!courseId.value) return;
  const res = await fetch(`/api/question/export?courseId=${courseId.value}`, { headers: authHeader() });
  const blob = await res.blob();
  downloadBlob(blob, `questions-${courseId.value}.xlsx`);
};

const doImport = async (opt: UploadRequestOptions) => {
  if (!courseId.value) return;
  importing.value = true;
  try {
    const fd = new FormData();
    fd.append('file', opt.file as File);
    await http.post('/api/question/import', fd, {
      params: { courseId: courseId.value },
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    await fetchData();
  } finally {
    importing.value = false;
  }
};

onMounted(async () => {
  await loadCourses();
  if (courses.value.length && !courseId.value) {
    courseId.value = courses.value[0].id;
    await onCourseChange();
  }
});
</script>
