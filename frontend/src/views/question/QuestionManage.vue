<template>
  <div class="qm-page">
    <div class="topbar">
      <h2 class="page-title">题目管理</h2>
      <el-button type="primary" :disabled="!selectedCourseId" @click="openCreate">
        + 新增题目
      </el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="toolbar-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索题目..."
          clearable
          style="width: 240px"
          @keyup.enter="fetchQuestions"
        />
        <el-select
          v-model="selectedCourseId"
          placeholder="所有课程"
          filterable
          clearable
          style="width: 180px"
          @change="onCourseChange"
        >
          <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
        </el-select>
        <el-select
          v-model="query.type"
          placeholder="所有类型"
          clearable
          style="width: 140px"
          @change="onFilterChange"
        >
          <el-option label="全部" :value="undefined" />
          <el-option label="单选题" value="SINGLE" />
          <el-option label="多选题" value="MULTIPLE" />
          <el-option label="判断题" value="TRUE_FALSE" />
          <el-option label="填空题" value="FILL" />
          <el-option label="简答题" value="SHORT" />
        </el-select>
        <el-select
          v-model="query.reviewStatus"
          placeholder="所有状态"
          clearable
          style="width: 140px"
          @change="onFilterChange"
        >
          <el-option label="全部" :value="undefined" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-tree-select
          v-model="selectedKpId"
          :data="kpTree"
          :props="{ label: 'name', children: 'children', value: 'id' }"
          clearable
          filterable
          placeholder="知识点"
          style="width: 180px"
          :disabled="!selectedCourseId"
          @change="onFilterChange"
        />
        <el-button type="primary" :disabled="!selectedCourseId" @click="fetchQuestions">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <div class="toolbar-row secondary">
        <el-button :disabled="!selectedCourseId" @click="onDownloadImportTemplate">下载模板</el-button>
        <el-upload
          :disabled="!selectedCourseId"
          :show-file-list="false"
          accept=".xlsx,.xls"
          :before-upload="beforeImportUpload"
        >
          <el-button :disabled="!selectedCourseId">导入题目</el-button>
        </el-upload>
        <el-button :disabled="!selectedCourseId" @click="onExport">导出题目</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="list-card" v-loading="loading">
      <template #header>
        <div class="list-header">题目列表（{{ total }}）</div>
      </template>

      <el-empty v-if="!list.length" description="暂无题目数据" />

      <div v-else class="question-list">
        <div v-for="(row, idx) in list" :key="row.id" class="question-item">
          <div class="q-main">
            <div class="q-title">
              <span class="q-id">第 {{ idx + 1 }} 题</span>
              <el-tag size="small" class="q-type">{{ typeLabel(row.type) }}</el-tag>
            </div>
            <div class="q-meta">
              <span>知识点: {{ row.knowledgePointId }}</span>
              <span>难度: {{ row.difficulty ?? '-' }}</span>
              <span>状态: {{ reviewLabel(row.reviewStatus) }}</span>
              <span>更新时间: {{ row.updateTime || '-' }}</span>
            </div>
            <div class="q-stem">
              {{ row.stem }}
            </div>
          </div>
          <div class="q-actions">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openVersions(row)">版本</el-button>
            <el-button
              v-if="row.reviewStatus === 'DRAFT' || row.reviewStatus === 'REJECTED'"
              link
              type="primary"
              @click="onSubmitReview(row)"
            >
              提交审核
            </el-button>
            <el-button
              v-if="isAdmin && row.reviewStatus === 'PENDING'"
              link
              type="success"
              @click="onApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="isAdmin && row.reviewStatus === 'PENDING'"
              link
              type="warning"
              @click="onReject(row)"
            >
              驳回
            </el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </div>
        </div>
      </div>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchQuestions"
          @size-change="fetchQuestions"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="editVisible"
      :title="editingId ? '编辑试题' : '新增试题'"
      width="720px"
      destroy-on-close
      @closed="resetEdit"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="96px">
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="editForm.courseId" placeholder="请选择课程" filterable style="width: 100%">
            <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点" prop="knowledgePointId">
          <el-tree-select
            v-model="editForm.knowledgePointId"
            :data="editKpTree"
            :props="{ label: 'name', children: 'children', value: 'id' }"
            filterable
            placeholder="请选择知识点"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="题型" prop="type">
          <el-select v-model="editForm.type" placeholder="请选择题型" style="width: 100%">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTIPLE" />
            <el-option label="判断题" value="TRUE_FALSE" />
            <el-option label="填空题" value="FILL" />
            <el-option label="简答题" value="SHORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" prop="stem">
          <el-input
            v-model="editForm.stem"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <template v-if="editForm.type === 'SINGLE'">
          <el-form-item label="选项 A" prop="singleOptions.A">
            <el-input v-model="singleOptions.A" placeholder="请输入选项 A" />
          </el-form-item>
          <el-form-item label="选项 B" prop="singleOptions.B">
            <el-input v-model="singleOptions.B" placeholder="请输入选项 B" />
          </el-form-item>
          <el-form-item label="选项 C" prop="singleOptions.C">
            <el-input v-model="singleOptions.C" placeholder="请输入选项 C" />
          </el-form-item>
          <el-form-item label="选项 D" prop="singleOptions.D">
            <el-input v-model="singleOptions.D" placeholder="请输入选项 D" />
          </el-form-item>
          <el-form-item label="正确答案" prop="answer">
            <el-radio-group v-model="editForm.answer">
              <el-radio label="A">A</el-radio>
              <el-radio label="B">B</el-radio>
              <el-radio label="C">C</el-radio>
              <el-radio label="D">D</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <template v-else-if="editForm.type === 'MULTIPLE'">
          <el-form-item label="选项 A" prop="singleOptions.A">
            <el-input v-model="singleOptions.A" placeholder="请输入选项 A" />
          </el-form-item>
          <el-form-item label="选项 B" prop="singleOptions.B">
            <el-input v-model="singleOptions.B" placeholder="请输入选项 B" />
          </el-form-item>
          <el-form-item label="选项 C" prop="singleOptions.C">
            <el-input v-model="singleOptions.C" placeholder="请输入选项 C" />
          </el-form-item>
          <el-form-item label="选项 D" prop="singleOptions.D">
            <el-input v-model="singleOptions.D" placeholder="请输入选项 D" />
          </el-form-item>
          <el-form-item label="正确答案" prop="answer">
            <el-checkbox-group v-model="multipleAnswers" @change="onMultipleAnswerChange">
              <el-checkbox label="A">A</el-checkbox>
              <el-checkbox label="B">B</el-checkbox>
              <el-checkbox label="C">C</el-checkbox>
              <el-checkbox label="D">D</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </template>
        <el-form-item v-if="editForm.type !== 'SINGLE' && editForm.type !== 'MULTIPLE'" label="答案" prop="answer">
          <el-radio-group
            v-if="editForm.type === 'TRUE_FALSE'"
            v-model="editForm.answer"
          >
            <el-radio label="A">对</el-radio>
            <el-radio label="B">错</el-radio>
          </el-radio-group>
          <el-input
            v-else
            v-model="editForm.answer"
            type="textarea"
            :rows="2"
            placeholder="主观题可填参考答案"
          />
        </el-form-item>
        <el-form-item label="解析" prop="analysis">
          <el-input v-model="editForm.analysis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="默认分值" prop="scoreDefault">
          <el-input-number v-model="editForm.scoreDefault" :min="0" :max="999" :step="1" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-input-number v-model="editForm.difficulty" :min="1" :max="5" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="versionVisible" title="历史版本" width="720px" destroy-on-close>
      <el-table :data="versionList" stripe>
        <el-table-column prop="versionNo" label="版本号" width="90" />
        <el-table-column prop="type" label="题型" width="96">
          <template #default="{ row }">
            {{ typeLabel(row.type) }}
          </template>
        </el-table-column>
        <el-table-column label="题干" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.stem }}
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="80" />
        <el-table-column prop="reviewStatus" label="审核状态" width="96">
          <template #default="{ row }">
            {{ reviewLabel(row.reviewStatus) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, watch } from 'vue';
import type { FormInstance, FormRules, UploadRawFile } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '@/api/http';
import {
  fetchQuestionPage,
  exportQuestionsBlob,
  downloadQuestionImportTemplateBlob,
  importQuestions,
  submitQuestionReview,
  approveQuestion,
  rejectQuestion,
  listQuestionVersions,
  type QuestionVersionVO
} from '@/api/modules/question';

interface CourseRow {
  id: number;
  name: string;
  code?: string;
}

interface CoursePage {
  records: CourseRow[];
  total: number;
}

interface KpRow {
  id: number;
  courseId: number;
  parentId: number | null;
  name: string;
  sortOrder: number;
  createTime?: string;
  children?: KpRow[];
}

interface QuestionRow {
  id: number;
  courseId: number;
  knowledgePointId: number;
  type: string;
  stem: string;
  optionsJson?: string;
  answer: string;
  analysis?: string;
  scoreDefault?: number;
  difficulty?: number;
  status: number;
  reviewStatus?: string;
  updateTime?: string;
}

interface QuestionPage {
  records: QuestionRow[];
  total: number;
}

const role = computed(() => localStorage.getItem('role') || '');
const isAdmin = computed(() => role.value === 'ADMIN');

const courseList = ref<CourseRow[]>([]);
const selectedCourseId = ref<number | undefined>(undefined);
const kpTree = ref<KpRow[]>([]);
const selectedKpId = ref<number | undefined>(undefined);
const editKpTree = ref<KpRow[]>([]);

const query = reactive({
  type: undefined as string | undefined,
  keyword: '',
  reviewStatus: undefined as string | undefined
});

const list = ref<QuestionRow[]>([]);
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
  knowledgePointId: undefined as number | undefined,
  type: '' as string,
  stem: '',
  optionsJson: '',
  answer: '',
  analysis: '',
  scoreDefault: 10,
  difficulty: 1
});
const singleOptions = reactive({
  A: '',
  B: '',
  C: '',
  D: ''
});
const multipleAnswers = ref<string[]>([]);

const editRules: FormRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  knowledgePointId: [{ required: true, message: '请选择知识点', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  stem: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }]
};

const versionVisible = ref(false);
const versionList = ref<QuestionVersionVO[]>([]);

function courseLabel(c: CourseRow) {
  if (c.code) return `${c.name}（${c.code}）`;
  return c.name;
}

function typeLabel(t: string | undefined) {
  switch (t) {
    case 'SINGLE':
      return '单选';
    case 'MULTIPLE':
      return '多选';
    case 'TRUE_FALSE':
      return '判断';
    case 'FILL':
      return '填空';
    case 'SHORT':
      return '简答';
    default:
      return t || '-';
  }
}

function reviewLabel(t: string | undefined) {
  switch (t) {
    case 'DRAFT':
      return '草稿';
    case 'PENDING':
      return '待审核';
    case 'PUBLISHED':
      return '已发布';
    case 'REJECTED':
      return '已驳回';
    default:
      return t || '-';
  }
}

function reviewTagType(t: string | undefined) {
  switch (t) {
    case 'DRAFT':
      return 'info';
    case 'PENDING':
      return 'warning';
    case 'PUBLISHED':
      return 'success';
    case 'REJECTED':
      return 'danger';
    default:
      return '';
  }
}

function buildKpTree(items: KpRow[]): KpRow[] {
  const map = new Map<number, KpRow>();
  items.forEach((i) => map.set(i.id, { ...i, children: [] }));
  const roots: KpRow[] = [];
  for (const i of items) {
    const node = map.get(i.id)!;
    const pid = i.parentId;
    if (pid == null) {
      roots.push(node);
      continue;
    }
    const parent = map.get(pid);
    if (parent) {
      parent.children!.push(node);
    } else {
      roots.push(node);
    }
  }
  const sortRec = (nodes: KpRow[]) => {
    nodes.sort((a, b) => (a.sortOrder - b.sortOrder) || a.id - b.id);
    nodes.forEach((n) => {
      if (n.children?.length) sortRec(n.children);
      else delete n.children;
    });
  };
  sortRec(roots);
  return roots;
}

async function fetchCourses() {
  const { data } = await http.get<CoursePage>('/api/course', {
    params: { page: 1, size: 200 }
  });
  courseList.value = data?.records ?? [];
  if (!selectedCourseId.value && courseList.value.length) {
    selectedCourseId.value = courseList.value[0].id;
  }
}

async function fetchKnowledgePoints() {
  if (!selectedCourseId.value) {
    kpTree.value = [];
    return;
  }
  kpTree.value = await fetchKnowledgePointsByCourse(selectedCourseId.value);
}

async function fetchKnowledgePointsByCourse(courseId: number): Promise<KpRow[]> {
  const { data } = await http.get<KpRow[]>('/api/knowledge-point', {
    params: { courseId }
  });
  const list = (data ?? []).map((k) => ({
    ...k,
    parentId: k.parentId ?? null
  }));
  return buildKpTree(list);
}

const fetchQuestions = async () => {
  if (!selectedCourseId.value) {
    list.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const { data } = await fetchQuestionPage({
      courseId: selectedCourseId.value,
      knowledgePointId: selectedKpId.value,
      type: query.type || undefined,
      keyword: query.keyword.trim() || undefined,
      reviewStatus: query.reviewStatus || undefined,
      page: page.value,
      size: size.value
    });
    const p = data as QuestionPage | undefined;
    list.value = p?.records ?? [];
    total.value = p?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

function resetFilters() {
  query.type = undefined;
  query.keyword = '';
  query.reviewStatus = undefined;
  selectedKpId.value = undefined;
  page.value = 1;
  fetchQuestions();
}

function onFilterChange() {
  page.value = 1;
  fetchQuestions();
}

function onCourseChange() {
  selectedKpId.value = undefined;
  page.value = 1;
  fetchKnowledgePoints();
  fetchQuestions();
}

function openCreate() {
  editingId.value = null;
  editForm.courseId = selectedCourseId.value;
  editForm.knowledgePointId = selectedKpId.value;
  editForm.type = '';
  editForm.stem = '';
  editForm.optionsJson = '';
  editForm.answer = '';
  editForm.analysis = '';
  editForm.scoreDefault = 10;
  editForm.difficulty = 1;
  singleOptions.A = '';
  singleOptions.B = '';
  singleOptions.C = '';
  singleOptions.D = '';
  multipleAnswers.value = [];
  editFormRef.value?.clearValidate();
  editVisible.value = true;
}

function parseSingleOptions(optionsJson?: string) {
  singleOptions.A = '';
  singleOptions.B = '';
  singleOptions.C = '';
  singleOptions.D = '';
  if (!optionsJson) return;
  try {
    const arr = JSON.parse(optionsJson) as unknown;
    if (!Array.isArray(arr)) return;
    const vals = arr.map((x) => String(x ?? '').replace(/^[A-Da-d][\.\)、\s]+/, '').trim());
    singleOptions.A = vals[0] || '';
    singleOptions.B = vals[1] || '';
    singleOptions.C = vals[2] || '';
    singleOptions.D = vals[3] || '';
  } catch {
    // ignore malformed json
  }
}

function parseMultipleAnswer(answer?: string) {
  if (!answer) {
    multipleAnswers.value = [];
    return;
  }
  multipleAnswers.value = answer
    .split(/[,，;；\s]+/)
    .map((s) => s.trim().toUpperCase())
    .filter((s) => ['A', 'B', 'C', 'D'].includes(s));
}

function onMultipleAnswerChange(vals: string[]) {
  editForm.answer = [...vals]
    .map((v) => v.trim().toUpperCase())
    .filter((v) => ['A', 'B', 'C', 'D'].includes(v))
    .sort()
    .join(',');
}

function openEdit(row: QuestionRow) {
  editingId.value = row.id;
  editForm.courseId = row.courseId;
  editForm.knowledgePointId = row.knowledgePointId;
  editForm.type = row.type;
  editForm.stem = row.stem;
  editForm.optionsJson = row.optionsJson ?? '';
  editForm.answer = row.answer;
  editForm.analysis = row.analysis ?? '';
  editForm.scoreDefault = row.scoreDefault ?? 10;
  editForm.difficulty = row.difficulty ?? 1;
  if (row.type === 'SINGLE') {
    parseSingleOptions(row.optionsJson);
    multipleAnswers.value = [];
  } else if (row.type === 'MULTIPLE') {
    parseSingleOptions(row.optionsJson);
    parseMultipleAnswer(row.answer);
  } else {
    singleOptions.A = '';
    singleOptions.B = '';
    singleOptions.C = '';
    singleOptions.D = '';
    multipleAnswers.value = [];
  }
  editFormRef.value?.clearValidate();
  editVisible.value = true;
}

function resetEdit() {
  editingId.value = null;
  editFormRef.value?.resetFields();
}

const submitEdit = async () => {
  const el = editFormRef.value;
  if (!el) return;
  await el.validate(async (valid) => {
    if (!valid) return;
    editSubmitting.value = true;
    try {
      if (editForm.type === 'SINGLE') {
        const a = singleOptions.A.trim();
        const b = singleOptions.B.trim();
        const c = singleOptions.C.trim();
        const d = singleOptions.D.trim();
        if (!a || !b || !c || !d) {
          ElMessage.warning('单选题请完整填写 A/B/C/D 四个选项');
          return;
        }
        if (!['A', 'B', 'C', 'D'].includes(editForm.answer.trim())) {
          ElMessage.warning('单选题请选择 A/B/C/D 其中一个作为答案');
          return;
        }
        editForm.optionsJson = JSON.stringify([a, b, c, d]);
      }
      if (editForm.type === 'MULTIPLE') {
        const a = singleOptions.A.trim();
        const b = singleOptions.B.trim();
        const c = singleOptions.C.trim();
        const d = singleOptions.D.trim();
        if (!a || !b || !c || !d) {
          ElMessage.warning('多选题请完整填写 A/B/C/D 四个选项');
          return;
        }
        if (!multipleAnswers.value.length) {
          ElMessage.warning('多选题请至少选择一个正确答案');
          return;
        }
        editForm.optionsJson = JSON.stringify([a, b, c, d]);
        editForm.answer = [...multipleAnswers.value].sort().join(',');
      }
      if (editingId.value == null) {
        await http.post('/api/question', {
          courseId: editForm.courseId,
          knowledgePointId: editForm.knowledgePointId,
          type: editForm.type,
          stem: editForm.stem.trim(),
          optionsJson: editForm.type === 'TRUE_FALSE'
            ? JSON.stringify(['对', '错'])
            : (editForm.optionsJson || undefined),
          answer: editForm.answer.trim(),
          analysis: editForm.analysis.trim() || undefined,
          scoreDefault: editForm.scoreDefault,
          difficulty: editForm.difficulty
        });
        ElMessage.success('创建成功');
      } else {
        await http.put(`/api/question/${editingId.value}`, {
          knowledgePointId: editForm.knowledgePointId,
          type: editForm.type,
          stem: editForm.stem.trim(),
          optionsJson: editForm.type === 'TRUE_FALSE'
            ? JSON.stringify(['对', '错'])
            : (editForm.optionsJson || undefined),
          answer: editForm.answer.trim(),
          analysis: editForm.analysis.trim() || undefined,
          scoreDefault: editForm.scoreDefault,
          difficulty: editForm.difficulty
        });
        ElMessage.success('保存成功');
      }
      editVisible.value = false;
      await fetchQuestions();
    } finally {
      editSubmitting.value = false;
    }
  });
};

async function onDelete(row: QuestionRow) {
  try {
    await ElMessageBox.confirm(`确定删除该试题（ID:${row.id}）？`, '删除试题', {
      type: 'warning'
    });
  } catch {
    return;
  }
  await http.delete(`/api/question/${row.id}`);
  ElMessage.success('已删除');
  await fetchQuestions();
}

async function onSubmitReview(row: QuestionRow) {
  await submitQuestionReview(row.id);
  ElMessage.success('已提交审核');
  await fetchQuestions();
}

async function onApprove(row: QuestionRow) {
  await approveQuestion(row.id);
  ElMessage.success('已通过');
  await fetchQuestions();
}

async function onReject(row: QuestionRow) {
  await rejectQuestion(row.id);
  ElMessage.success('已驳回');
  await fetchQuestions();
}

async function onExport() {
  if (!selectedCourseId.value) return;
  const blob = await exportQuestionsBlob(selectedCourseId.value);
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = '题库导出.xlsx';
  a.click();
  window.URL.revokeObjectURL(url);
}

async function onDownloadImportTemplate() {
  const blob = await downloadQuestionImportTemplateBlob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = '试题导入模板.xlsx';
  a.click();
  window.URL.revokeObjectURL(url);
}

const beforeImportUpload = async (file: UploadRawFile) => {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择课程');
    return false;
  }
  try {
    await importQuestions(selectedCourseId.value, file as unknown as File);
    ElMessage.success('导入成功');
    await fetchQuestions();
  } catch (e) {
    // 错误信息由后端统一提示
  }
  return false;
};

async function openVersions(row: QuestionRow) {
  const { data } = await listQuestionVersions(row.id);
  versionList.value = data ?? [];
  versionVisible.value = true;
}

onMounted(async () => {
  await fetchCourses();
  await fetchKnowledgePoints();
  editKpTree.value = selectedCourseId.value ? await fetchKnowledgePointsByCourse(selectedCourseId.value) : [];
  await fetchQuestions();
});

watch(
  () => editForm.courseId,
  async (cid, prev) => {
    if (!cid) {
      editKpTree.value = [];
      editForm.knowledgePointId = undefined;
      return;
    }
    // 切换课程时，重置知识点，避免跨课程残留
    if (prev && prev !== cid) {
      editForm.knowledgePointId = undefined;
    }
    editKpTree.value = await fetchKnowledgePointsByCourse(cid);
  }
);
</script>

<style scoped>
.qm-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topbar {
  display: flex;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
}

.filter-card,
.list-card {
  border-radius: 10px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar-row.secondary {
  margin-top: 10px;
}

.list-header {
  font-size: 20px;
  font-weight: 600;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.question-item {
  padding: 14px 2px;
  border-bottom: 1px solid #eef0f4;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  min-height: 102px;
}

.question-item:last-child {
  border-bottom: none;
}

.q-main {
  flex: 1;
  min-width: 0;
}

.q-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.q-id {
  font-size: 18px;
  font-weight: 600;
}

.q-type {
  transform: translateY(1px);
}

.q-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 8px;
}

.q-stem {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.q-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
  min-width: 230px;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>

