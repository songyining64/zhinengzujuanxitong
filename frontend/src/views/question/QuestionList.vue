<template>
  <div class="question-split">
    <aside v-if="courseId" class="filter-aside">
      <el-card shadow="never" class="filter-card">
        <template #header>分类筛选</template>
        <el-form label-position="top" size="small">
          <el-form-item label="题型">
            <el-select v-model="filters.type" clearable placeholder="全部" style="width: 100%">
              <el-option label="单选" value="SINGLE" />
              <el-option label="多选" value="MULTIPLE" />
              <el-option label="判断" value="TRUE_FALSE" />
              <el-option label="填空" value="FILL" />
              <el-option label="简答" value="SHORT" />
            </el-select>
          </el-form-item>
          <el-form-item label="审核状态">
            <el-select v-model="filters.reviewStatus" clearable placeholder="全部" style="width: 100%">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="待审" value="PENDING" />
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="驳回" value="REJECTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识点">
            <el-select v-model="filters.knowledgePointId" clearable filterable placeholder="全部" style="width: 100%">
              <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" clearable placeholder="题干" @keyup.enter="fetchData" />
          </el-form-item>
          <el-button type="primary" style="width: 100%" @click="fetchData">查询</el-button>
          <el-button style="width: 100%; margin: 8px 0 0" @click="resetFilters">重置</el-button>
        </el-form>
      </el-card>
    </aside>

    <section class="main-col">
      <el-card shadow="never">
        <template #header>
          <div class="head">
            <span>试题列表</span>
            <CoursePicker />
          </div>
        </template>

        <el-alert v-if="!courseId" type="info" show-icon :closable="false" title="请先在顶栏选择课程" />

        <template v-else>
          <div class="toolbar">
            <el-button type="primary" @click="openEdit(null)">新建试题</el-button>
            <el-button :disabled="!selection.length" @click="openBatch">批量修改</el-button>
            <el-upload :show-file-list="false" accept=".xlsx,.xls" :http-request="onImport">
              <el-button type="success">Excel 导入</el-button>
            </el-upload>
            <el-button @click="onExport">导出</el-button>
            <el-button @click="downloadTpl">导入模板</el-button>
          </div>

          <el-table v-loading="loading" :data="list" stripe @selection-change="selection = $event">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column min-width="200" show-overflow-tooltip>
              <template #header>
                <div class="course-col-head">
                  <span class="course-head-title">课程</span>
                  <el-select
                    :model-value="courseId ?? undefined"
                    placeholder="选择课程"
                    filterable
                    size="small"
                    class="course-head-select"
                    @update:model-value="onTableCourseChange"
                  >
                    <el-option
                      v-for="c in coursesList"
                      :key="c.id"
                      :label="courseOptionLabel(c)"
                      :value="c.id"
                    />
                  </el-select>
                </div>
              </template>
              <template #default="{ row }">
                <span>{{ courseDisplayName(row) }}</span>
                <el-tag v-if="isSampleCourseId(row.courseId)" size="small" type="info" class="course-tag">示例</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="题型" width="88" />
            <el-table-column label="题干摘要" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ stemPreview(row.stem) }}</template>
            </el-table-column>
            <el-table-column prop="difficulty" label="难度" width="72" />
            <el-table-column prop="reviewStatus" label="审核" width="88" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="primary" @click="openDedup(row)">查重</el-button>
                <el-button
                  v-if="row.reviewStatus === 'DRAFT' || row.reviewStatus === 'REJECTED'"
                  link
                  type="primary"
                  @click="submitReview(row)"
                >
                  提交审核
                </el-button>
                <el-button v-if="isAdmin" link type="success" @click="approve(row)">通过</el-button>
                <el-button v-if="isAdmin" link type="warning" @click="reject(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="size"
              layout="total, prev, pager, next"
              :total="total"
              @current-change="fetchData"
            />
          </div>
        </template>
      </el-card>
    </section>

    <el-drawer v-model="editDlg.visible" :title="editDlg.id ? '编辑试题' : '新建试题'" size="640px" destroy-on-close @closed="onDrawerClosed">
      <div class="drawer-body">
        <el-form label-position="top">
          <el-form-item label="知识点" required>
            <el-select v-model="editDlg.form.knowledgePointId" filterable style="width: 100%">
              <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型" required>
            <el-select v-model="editDlg.form.type" style="width: 220px">
              <el-option label="单选 SINGLE" value="SINGLE" />
              <el-option label="多选 MULTIPLE" value="MULTIPLE" />
              <el-option label="判断 TRUE_FALSE" value="TRUE_FALSE" />
              <el-option label="填空 FILL" value="FILL" />
              <el-option label="简答 SHORT" value="SHORT" />
            </el-select>
          </el-form-item>
          <el-form-item label="题干（富文本）" required>
            <RichEditor v-model="editDlg.form.stem" />
          </el-form-item>
          <el-form-item label="选项 JSON">
            <el-input v-model="editDlg.form.optionsJson" type="textarea" rows="3" placeholder='["选项A","选项B"]' />
          </el-form-item>
          <el-form-item label="答案">
            <el-input v-model="editDlg.form.answer" type="textarea" rows="2" />
          </el-form-item>
          <el-form-item label="解析">
            <el-input v-model="editDlg.form.analysis" type="textarea" rows="2" />
          </el-form-item>
          <el-form-item label="默认分值">
            <el-input-number v-model="editDlg.form.scoreDefault" :min="0" :step="1" />
          </el-form-item>
          <el-form-item label="难度 1-3">
            <el-input-number v-model="editDlg.form.difficulty" :min="1" :max="3" />
          </el-form-item>
        </el-form>
        <div class="drawer-footer">
          <el-button @click="runDedupFromForm" :disabled="!stemPlain(editDlg.form.stem)">查重预览</el-button>
          <el-button @click="editDlg.visible = false">取消</el-button>
          <el-button type="primary" :loading="editDlg.saving" @click="saveQuestion">保存（实时编辑）</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="batchDlg.visible" title="批量修改" width="400px">
      <el-form label-width="100px">
        <el-form-item label="知识点">
          <el-select v-model="batchDlg.knowledgePointId" clearable filterable style="width: 100%">
            <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-input-number v-model="batchDlg.difficulty" :min="1" :max="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="batchDlg.saving" @click="runBatch">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dedupDlg.visible" title="查重结果" width="640px">
      <p v-if="dedupDlg.result" class="dedup-meta">
        比对 {{ dedupDlg.result.comparedTotal }} 题
        <span v-if="dedupDlg.result.truncated">（已截断）</span>
      </p>
      <el-table v-if="dedupDlg.result?.hits?.length" :data="dedupDlg.result.hits" size="small">
        <el-table-column prop="questionId" label="题目ID" width="90" />
        <el-table-column prop="similarity" label="相似度" width="90">
          <template #default="{ row }">{{ (row.similarity * 100).toFixed(1) }}%</template>
        </el-table-column>
        <el-table-column prop="type" label="题型" width="80" />
        <el-table-column prop="stemPreview" label="题干摘要" />
      </el-table>
      <el-empty v-else description="未发现相近题目" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import type { UploadRequestOptions } from 'element-plus';
import CoursePicker from '@/components/CoursePicker.vue';
import RichEditor from '@/components/RichEditor.vue';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchCoursePage } from '@/api/modules/course';
import { fetchKnowledgeList } from '@/api/modules/knowledge';
import {
  fetchQuestionPage,
  saveQuestion as apiSaveQuestion,
  batchUpdateQuestions,
  importQuestions,
  exportQuestionsBlob,
  downloadQuestionImportTemplateBlob,
  submitQuestionReview,
  approveQuestion,
  rejectQuestion,
  dedupCheck
} from '@/api/modules/question';
import { mergeCoursesWithSamples, isSampleCourseId } from '@/data/sampleCourses';
import type { Course, Question } from '@/types/models';
import type { QuestionSaveBody } from '@/api/modules/question';
import { stripHtml } from '@/utils/html';

const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

const role = computed(() => localStorage.getItem('role') || '');
const isAdmin = computed(() => role.value === 'ADMIN');

const knowledgePoints = ref<{ id: number; name: string }[]>([]);
/** 与课程管理 / 课程选择器一致，用于列表展示课程名 */
const coursesList = ref<Course[]>([]);

const courseById = computed(() => {
  const m = new Map<number, Course>();
  for (const c of coursesList.value) {
    m.set(c.id, c);
  }
  return m;
});

function courseDisplayName(row: Question) {
  const c = courseById.value.get(row.courseId);
  if (c) return c.name;
  return `课程 #${row.courseId}`;
}

function courseOptionLabel(c: Course) {
  return isSampleCourseId(c.id) ? `${c.name}（示例）` : c.name;
}

function onTableCourseChange(id: number | undefined) {
  store.setCourseId(id ?? null);
}

async function loadCoursesForTable() {
  try {
    const { data } = await fetchCoursePage(1, 500);
    coursesList.value = mergeCoursesWithSamples(data?.records ?? []);
  } catch {
    coursesList.value = mergeCoursesWithSamples([]);
  }
}

onMounted(() => {
  void loadCoursesForTable();
});

const loading = ref(false);
const list = ref<Question[]>([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);
const selection = ref<Question[]>([]);

const filters = reactive({
  type: '' as string,
  reviewStatus: '' as string,
  knowledgePointId: undefined as number | undefined,
  keyword: ''
});

function stemPreview(stem: string) {
  const t = stripHtml(stem);
  return t.length > 80 ? t.slice(0, 80) + '…' : t || '（空）';
}

function stemPlain(html: string) {
  return stripHtml(html || '');
}

async function loadKnowledge() {
  if (!courseId.value) {
    knowledgePoints.value = [];
    return;
  }
  const { data } = await fetchKnowledgeList(courseId.value);
  knowledgePoints.value = (data ?? []).map((k) => ({ id: k.id, name: k.name }));
}

function resetFilters() {
  filters.type = '';
  filters.reviewStatus = '';
  filters.knowledgePointId = undefined;
  filters.keyword = '';
  page.value = 1;
  fetchData();
}

async function fetchData() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchQuestionPage({
      courseId: courseId.value,
      type: filters.type || undefined,
      reviewStatus: filters.reviewStatus || undefined,
      knowledgePointId: filters.knowledgePointId,
      keyword: filters.keyword || undefined,
      page: page.value,
      size: size.value
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

watch(courseId, async () => {
  await loadKnowledge();
  list.value = [];
  total.value = 0;
  if (courseId.value) await fetchData();
}, { immediate: true });

const editDlg = reactive({
  visible: false,
  id: null as number | null,
  saving: false,
  form: {
    knowledgePointId: 0,
    type: 'SINGLE',
    stem: '<p><br></p>',
    optionsJson: '',
    answer: '',
    analysis: '',
    scoreDefault: 5 as number | undefined,
    difficulty: 2 as number | undefined
  }
});

function onDrawerClosed() {
  /* 抽屉销毁后重置由 destroy-on-close 处理 */
}

function openEdit(row: Question | null) {
  if (!courseId.value) return;
  editDlg.id = row?.id ?? null;
  if (row) {
    const raw = row.stem || '';
    editDlg.form = {
      knowledgePointId: row.knowledgePointId,
      type: row.type,
      stem: raw.includes('<') ? raw : `<p>${raw.replace(/\n/g, '<br/>')}</p>`,
      optionsJson: row.optionsJson || '',
      answer: row.answer || '',
      analysis: row.analysis || '',
      scoreDefault: row.scoreDefault != null ? Number(row.scoreDefault) : 5,
      difficulty: row.difficulty ?? 2
    };
  } else {
    editDlg.form = {
      knowledgePointId: knowledgePoints.value[0]?.id ?? 0,
      type: 'SINGLE',
      stem: '<p><br></p>',
      optionsJson: '["","","",""]',
      answer: '',
      analysis: '',
      scoreDefault: 5,
      difficulty: 2
    };
  }
  editDlg.visible = true;
}

async function saveQuestion() {
  if (!courseId.value || !editDlg.form.knowledgePointId) {
    ElMessage.warning('请填写知识点');
    return;
  }
  if (!stemPlain(editDlg.form.stem)) {
    ElMessage.warning('请填写题干');
    return;
  }
  const body: QuestionSaveBody = {
    courseId: courseId.value,
    knowledgePointId: editDlg.form.knowledgePointId,
    type: editDlg.form.type,
    stem: editDlg.form.stem,
    optionsJson: editDlg.form.optionsJson || undefined,
    answer: editDlg.form.answer || undefined,
    analysis: editDlg.form.analysis || undefined,
    scoreDefault: editDlg.form.scoreDefault,
    difficulty: editDlg.form.difficulty
  };
  if (editDlg.id) body.id = editDlg.id;
  editDlg.saving = true;
  try {
    await apiSaveQuestion(body);
    ElMessage.success('已保存');
    editDlg.visible = false;
    await fetchData();
  } finally {
    editDlg.saving = false;
  }
}

const batchDlg = reactive({
  visible: false,
  saving: false,
  knowledgePointId: undefined as number | undefined,
  difficulty: undefined as number | undefined
});

function openBatch() {
  batchDlg.knowledgePointId = undefined;
  batchDlg.difficulty = undefined;
  batchDlg.visible = true;
}

async function runBatch() {
  if (!courseId.value || !selection.value.length) return;
  batchDlg.saving = true;
  try {
    await batchUpdateQuestions({
      courseId: courseId.value,
      questionIds: selection.value.map((q) => q.id),
      knowledgePointId: batchDlg.knowledgePointId,
      difficulty: batchDlg.difficulty
    });
    ElMessage.success('已更新');
    batchDlg.visible = false;
    await fetchData();
  } finally {
    batchDlg.saving = false;
  }
}

async function onImport(req: UploadRequestOptions) {
  if (!courseId.value || !req.file) return;
  try {
    await importQuestions(courseId.value, req.file as File);
    ElMessage.success('导入请求已提交');
    await fetchData();
  } catch {
    /* */
  }
}

async function onExport() {
  if (!courseId.value) return;
  try {
    const blob = await exportQuestionsBlob(courseId.value);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `questions-${courseId.value}.xlsx`;
    a.click();
    URL.revokeObjectURL(url);
  } catch {
    ElMessage.error('导出失败');
  }
}

async function downloadTpl() {
  try {
    const blob = await downloadQuestionImportTemplateBlob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'question-import-template.xlsx';
    a.click();
    URL.revokeObjectURL(url);
  } catch {
    ElMessage.error('下载失败');
  }
}

async function submitReview(row: Question) {
  await submitQuestionReview(row.id);
  ElMessage.success('已提交审核');
  await fetchData();
}

async function approve(row: Question) {
  await approveQuestion(row.id);
  ElMessage.success('已通过');
  await fetchData();
}

async function reject(row: Question) {
  await rejectQuestion(row.id);
  ElMessage.success('已驳回');
  await fetchData();
}

const dedupDlg = reactive({
  visible: false,
  result: null as import('@/types/models').QuestionDedupResultVO | null
});

async function openDedup(row: Question) {
  if (!courseId.value) return;
  const { data } = await dedupCheck({
    courseId: courseId.value,
    stem: row.stem,
    optionsJson: row.optionsJson,
    excludeQuestionId: row.id,
    knowledgePointId: row.knowledgePointId
  });
  dedupDlg.result = data ?? null;
  dedupDlg.visible = true;
}

async function runDedupFromForm() {
  if (!courseId.value) return;
  const plain = stemPlain(editDlg.form.stem);
  if (!plain) return;
  const { data } = await dedupCheck({
    courseId: courseId.value,
    stem: editDlg.form.stem,
    optionsJson: editDlg.form.optionsJson || undefined,
    excludeQuestionId: editDlg.id ?? undefined,
    knowledgePointId: editDlg.form.knowledgePointId
  });
  dedupDlg.result = data ?? null;
  dedupDlg.visible = true;
}
</script>

<style scoped>
.question-split {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.filter-aside {
  flex: 0 0 240px;
  width: 100%;
  max-width: 280px;
}

.filter-card :deep(.el-card__header) {
  padding: 12px 16px;
  font-weight: 600;
}

.main-col {
  flex: 1;
  min-width: 0;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.course-tag {
  margin-left: 6px;
  vertical-align: middle;
}

.course-col-head {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  padding: 4px 0;
  line-height: 1.2;
}

.course-head-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.course-head-select {
  width: 100%;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.drawer-body {
  padding-bottom: 24px;
}

.drawer-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.dedup-meta {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

@media (max-width: 900px) {
  .question-split {
    flex-direction: column;
  }
  .filter-aside {
    flex: 1 1 auto;
    max-width: none;
  }
}
</style>
