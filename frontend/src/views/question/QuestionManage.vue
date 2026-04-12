<template>
  <div class="question-page">
    <!-- 顶部功能区 -->
    <div class="panel panel-top">
      <h1 class="page-title">题目管理</h1>
      <div class="top-actions">
        <el-button link type="primary" class="link-excel" @click="router.push('/question')">Excel 导入 / 导出</el-button>
        <el-button type="primary" size="large" class="btn-new" :disabled="!courseId" @click="openCreate">新增试题</el-button>
      </div>
    </div>

    <!-- 中部检索区 -->
    <div class="panel panel-search">
      <div class="filter-row filter-row-selects">
        <el-select v-model="courseId" class="f-course" placeholder="课程" filterable @change="onCourseChange">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filterKp" class="f-kp" clearable placeholder="知识点" @change="load">
          <el-option v-for="k in kpList" :key="k.id" :label="k.name" :value="k.id" />
        </el-select>
        <el-select v-model="filterType" class="f-type" clearable placeholder="题型" @change="load">
          <el-option label="单选" value="SINGLE" />
          <el-option label="多选" value="MULTIPLE" />
          <el-option label="判断" value="TRUE_FALSE" />
          <el-option label="填空" value="FILL" />
          <el-option label="简答" value="SHORT" />
        </el-select>
      </div>
      <div class="filter-row filter-row-keyword">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索题干关键词"
          clearable
          @keyup.enter="load"
        />
        <el-button type="primary" size="large" class="search-btn" @click="load">查询</el-button>
        <el-button size="large" :disabled="!courseId" @click="openDedup">题干查重</el-button>
        <el-button size="large" :disabled="!courseId || !selectedIds.length" @click="openBatch">批量调整</el-button>
      </div>
    </div>

    <!-- 数据展示区 -->
    <div class="panel panel-table">
      <el-table
        v-loading="loading"
        :data="rows"
        class="data-table"
        :header-cell-style="headerCellStyle"
        fit
        @selection-change="onSelectionChange"
      >
        <template #empty>
          <div class="table-empty">No Data</div>
        </template>
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="id" label="ID" width="72" align="center" />
        <el-table-column prop="type" label="题型" width="100" align="center" />
        <el-table-column prop="stem" label="题干" min-width="220" show-overflow-tooltip />
        <el-table-column prop="reviewStatus" label="审核" width="100" align="center" />
        <el-table-column label="操作" min-width="300" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="openVersions(row)">版本</el-button>
            <el-button type="danger" link @click="onDelete(row)">删除</el-button>
            <el-button v-if="row.reviewStatus === 'DRAFT'" type="warning" link @click="onSubmitReview(row)">
              提交审核
            </el-button>
            <template v-if="isAdmin && row.reviewStatus === 'PENDING'">
              <el-button type="success" link @click="onApprove(row)">通过</el-button>
              <el-button type="info" link @click="onReject(row)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="editId ? '编辑试题' : '新增试题'" width="640px" destroy-on-close @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="知识点" required>
          <el-select v-model="form.knowledgePointId" placeholder="选择" filterable style="width: 100%">
            <el-option v-for="k in kpList" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="单选" value="SINGLE" />
            <el-option label="多选" value="MULTIPLE" />
            <el-option label="判断" value="TRUE_FALSE" />
            <el-option label="填空" value="FILL" />
            <el-option label="简答" value="SHORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="form.stem" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="选项 JSON">
          <el-input v-model="form.optionsJson" type="textarea" :rows="3" placeholder='客观题示例：["A","B","C","D"]' />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="form.answer" placeholder="单选/判断填字母；多选逗号分隔" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="默认分值">
          <el-input-number v-model="form.scoreDefault" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="难度">
          <el-input-number v-model="form.difficulty" :min="1" :max="3" />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="form.reviewStatus" style="width: 100%">
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待审" value="PENDING" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dedupVisible" title="题干相似度查重" width="640px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="题干" required>
          <el-input v-model="dedupStem" type="textarea" :rows="4" placeholder="与课程下已有试题比对相似度" />
        </el-form-item>
        <el-form-item label="选项 JSON">
          <el-input v-model="dedupOptionsJson" type="textarea" :rows="2" placeholder="可选，与题库 options_json 联合比对" />
        </el-form-item>
        <el-form-item label="知识点范围">
          <el-select v-model="dedupKpId" clearable placeholder="不选则全课程" style="width: 100%">
            <el-option v-for="k in kpList" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="dedupThreshold" :min="0.5" :max="1" :step="0.01" :precision="2" />
          <span class="hint">默认 0.78，越高越严格</span>
        </el-form-item>
      </el-form>
      <el-button type="primary" :loading="dedupLoading" :disabled="!dedupStem.trim()" @click="runDedup">检测</el-button>
      <el-table v-if="dedupHits.length" :data="dedupHits" class="dedup-table" size="small">
        <el-table-column prop="questionId" label="题目ID" width="88" />
        <el-table-column prop="similarity" label="相似度" width="88">
          <template #default="{ row }">{{ (Number(row.similarity) * 100).toFixed(1) }}%</template>
        </el-table-column>
        <el-table-column prop="type" label="题型" width="90" />
        <el-table-column prop="stemPreview" label="题干摘要" min-width="200" show-overflow-tooltip />
      </el-table>
      <p v-if="dedupMeta" class="dedup-meta">比对样本 {{ dedupMeta.comparedTotal }} 条{{ dedupMeta.truncated ? '（已截断）' : '' }}</p>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="批量调整" width="480px" destroy-on-close>
      <p class="batch-hint">已选 {{ selectedIds.length }} 道题，至少填写一项修改。</p>
      <el-form label-width="100px">
        <el-form-item label="难度">
          <el-select v-model="batchDifficulty" clearable placeholder="不修改" style="width: 100%">
            <el-option label="1" :value="1" />
            <el-option label="2" :value="2" />
            <el-option label="3" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="batchKpId" clearable placeholder="不修改" filterable style="width: 100%">
            <el-option v-for="k in kpList" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="runBatch">应用</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="verDrawer" :title="verTitle" size="420px" destroy-on-close @closed="versionRows = []">
      <el-table :data="versionRows" size="small" @row-click="onVersionRow">
        <el-table-column prop="versionNo" label="版本" width="72" />
        <el-table-column prop="reviewStatus" label="状态" width="88" />
        <el-table-column prop="createTime" label="时间" min-width="140" />
      </el-table>
      <p class="ver-tip">点击行查看该版本快照</p>
    </el-drawer>

    <el-dialog v-model="verDetailVisible" title="版本快照" width="560px" destroy-on-close>
      <el-descriptions v-if="verDetail" :column="1" border size="small">
        <el-descriptions-item label="版本">{{ verDetail.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="题型">{{ verDetail.type }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ verDetail.difficulty }}</el-descriptions-item>
        <el-descriptions-item label="审核">{{ verDetail.reviewStatus }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ verDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="题干">{{ verDetail.stem }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, watch } from 'vue';
import type { CSSProperties } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import { fetchKnowledgeList, type KpRow } from '@/api/knowledge';
import {
  fetchQuestionPage,
  getQuestion,
  saveQuestion,
  deleteQuestion,
  submitQuestionReview,
  approveQuestion,
  rejectQuestion,
  listQuestionVersions,
  fetchQuestionVersion,
  dedupCheckQuestion,
  batchUpdateQuestions,
  type QuestionVersionVO
} from '@/api/modules/question';
import { getLastCourseId, setLastCourseId } from '@/composables/useLastCourseId';
import { isRole } from '@/composables/usePermission';

const router = useRouter();
const route = useRoute();
const isAdmin = computed(() => isRole('ADMIN'));

const headerCellStyle = (): CSSProperties => ({
  background: '#f5f7fa',
  fontWeight: '700',
  textAlign: 'center',
  color: '#303133'
});

const courses = ref<CourseRow[]>([]);
const courseId = ref<number | null>(getLastCourseId());
const kpList = ref<KpRow[]>([]);
const filterKp = ref<number | undefined>();
const filterType = ref<string | undefined>();
const keyword = ref('');
const loading = ref(false);
const rows = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

const selectedRows = ref<Record<string, unknown>[]>([]);
const selectedIds = computed(() => selectedRows.value.map((r) => r.id as number));

const dlg = ref(false);
const editId = ref<number | null>(null);
const saving = ref(false);
const form = reactive({
  knowledgePointId: undefined as number | undefined,
  type: 'SINGLE',
  stem: '',
  optionsJson: '',
  answer: '',
  analysis: '',
  scoreDefault: 10,
  difficulty: 2,
  reviewStatus: 'PUBLISHED'
});

const dedupVisible = ref(false);
const dedupStem = ref('');
const dedupOptionsJson = ref('');
const dedupKpId = ref<number | undefined>();
const dedupThreshold = ref(0.78);
const dedupLoading = ref(false);
const dedupHits = ref<Record<string, unknown>[]>([]);
const dedupMeta = ref<{ comparedTotal: number; truncated: boolean } | null>(null);

const batchVisible = ref(false);
const batchDifficulty = ref<number | undefined>();
const batchKpId = ref<number | undefined>();
const batchLoading = ref(false);

const verDrawer = ref(false);
const verTitle = ref('版本历史');
const versionQid = ref<number | null>(null);
const versionRows = ref<QuestionVersionVO[]>([]);
const verDetailVisible = ref(false);
const verDetail = ref<QuestionVersionVO | null>(null);

function onSelectionChange(list: Record<string, unknown>[]) {
  selectedRows.value = list;
}

async function loadCourses() {
  const { data } = await fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
  }
}

async function onCourseChange() {
  filterKp.value = undefined;
  kpList.value = [];
  selectedRows.value = [];
  if (courseId.value) {
    setLastCourseId(courseId.value);
    const { data } = await fetchKnowledgeList(courseId.value);
    kpList.value = data ?? [];
  }
  await load();
}

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchQuestionPage({
      courseId: courseId.value,
      knowledgePointId: filterKp.value,
      type: filterType.value,
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    });
    rows.value = (data as { records?: Record<string, unknown>[] })?.records ?? [];
    total.value = (data as { total?: number })?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  editId.value = null;
  form.knowledgePointId = kpList.value[0]?.id;
  form.type = 'SINGLE';
  form.stem = '';
  form.optionsJson = '';
  form.answer = '';
  form.analysis = '';
  form.scoreDefault = 10;
  form.difficulty = 2;
  form.reviewStatus = 'PUBLISHED';
}

function openCreate() {
  resetForm();
  dlg.value = true;
}

async function openEdit(row: Record<string, unknown>) {
  editId.value = row.id as number;
  const { data } = await getQuestion(editId.value);
  const d = data as Record<string, unknown>;
  form.knowledgePointId = d.knowledgePointId as number;
  form.type = String(d.type);
  form.stem = String(d.stem ?? '');
  form.optionsJson = String(d.optionsJson ?? '');
  form.answer = String(d.answer ?? '');
  form.analysis = String(d.analysis ?? '');
  form.scoreDefault = Number(d.scoreDefault ?? 10);
  form.difficulty = Number(d.difficulty ?? 2);
  form.reviewStatus = String(d.reviewStatus ?? 'PUBLISHED');
  dlg.value = true;
}

async function save() {
  if (!courseId.value || !form.knowledgePointId || !form.stem.trim() || !form.answer.trim()) return;
  saving.value = true;
  try {
    const payload: Record<string, unknown> = {
      courseId: courseId.value,
      knowledgePointId: form.knowledgePointId,
      type: form.type,
      stem: form.stem,
      optionsJson: form.optionsJson || undefined,
      answer: form.answer,
      analysis: form.analysis || undefined,
      scoreDefault: form.scoreDefault,
      difficulty: form.difficulty,
      reviewStatus: form.reviewStatus
    };
    if (editId.value) {
      payload.id = editId.value;
    }
    await saveQuestion(payload);
    dlg.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function onDelete(row: Record<string, unknown>) {
  try {
    await ElMessageBox.confirm('确定删除该试题？', '删除', { type: 'warning' });
  } catch {
    return;
  }
  await deleteQuestion(row.id as number);
  await load();
}

function openDedup() {
  dedupStem.value = '';
  dedupOptionsJson.value = '';
  dedupKpId.value = filterKp.value;
  dedupHits.value = [];
  dedupMeta.value = null;
  dedupVisible.value = true;
}

async function runDedup() {
  if (!courseId.value || !dedupStem.value.trim()) return;
  dedupLoading.value = true;
  dedupHits.value = [];
  dedupMeta.value = null;
  try {
    const data = await dedupCheckQuestion({
      courseId: courseId.value,
      stem: dedupStem.value.trim(),
      optionsJson: dedupOptionsJson.value.trim() || undefined,
      knowledgePointId: dedupKpId.value,
      threshold: dedupThreshold.value
    });
    dedupHits.value = (data?.hits as Record<string, unknown>[]) ?? [];
    dedupMeta.value = {
      comparedTotal: data?.comparedTotal ?? 0,
      truncated: !!data?.truncated
    };
    if (!dedupHits.value.length) ElMessage.success('未发现明显高于阈值的相似题');
  } catch {
    /* http 拦截器已提示 */
  } finally {
    dedupLoading.value = false;
  }
}

function openBatch() {
  batchDifficulty.value = undefined;
  batchKpId.value = undefined;
  batchVisible.value = true;
}

async function runBatch() {
  if (!courseId.value || !selectedIds.value.length) return;
  if (batchDifficulty.value == null && batchKpId.value == null) {
    ElMessage.warning('请至少选择难度或知识点之一');
    return;
  }
  batchLoading.value = true;
  try {
    const n = await batchUpdateQuestions({
      courseId: courseId.value,
      questionIds: selectedIds.value,
      difficulty: batchDifficulty.value ?? undefined,
      knowledgePointId: batchKpId.value ?? undefined
    });
    ElMessage.success(`已更新 ${n ?? 0} 道题`);
    batchVisible.value = false;
    selectedRows.value = [];
    await load();
  } catch {
    /* */
  } finally {
    batchLoading.value = false;
  }
}

async function openVersions(row: Record<string, unknown>) {
  const id = row.id as number;
  versionQid.value = id;
  verTitle.value = `版本历史 · 题目 #${id}`;
  verDrawer.value = true;
  const list = await listQuestionVersions(id);
  versionRows.value = list ?? [];
}

async function onVersionRow(row: QuestionVersionVO) {
  if (!versionQid.value || row.versionNo == null) return;
  const v = await fetchQuestionVersion(versionQid.value, row.versionNo);
  verDetail.value = v ?? null;
  verDetailVisible.value = true;
}

async function onSubmitReview(row: Record<string, unknown>) {
  try {
    await ElMessageBox.confirm('提交后进入待审核，确定？', '提交审核');
  } catch {
    return;
  }
  await submitQuestionReview(row.id as number);
  ElMessage.success('已提交审核');
  await load();
}

async function onApprove(row: Record<string, unknown>) {
  await approveQuestion(row.id as number);
  ElMessage.success('已通过');
  await load();
}

async function onReject(row: Record<string, unknown>) {
  await rejectQuestion(row.id as number);
  ElMessage.success('已驳回');
  await load();
}

async function consumeNewActionIfPresent() {
  const act = route.query.action;
  const actionVal = Array.isArray(act) ? act[0] : act;
  if (actionVal !== 'new') return;

  if (!courses.value.length) {
    await loadCourses();
  }
  if (courseId.value && kpList.value.length === 0) {
    await onCourseChange();
  }

  router.replace({ path: route.path, query: {} });

  if (courseId.value) {
    openCreate();
  } else {
    ElMessage.warning('暂无课程，请先创建课程后再新增试题');
  }
}

onMounted(async () => {
  await loadCourses();
  await onCourseChange();
  await load();
  await consumeNewActionIfPresent();
});

watch(
  () => route.query.action,
  async (action) => {
    const actionVal = Array.isArray(action) ? action[0] : action;
    if (actionVal !== 'new') return;
    await consumeNewActionIfPresent();
  }
);
</script>

<style scoped>
.question-page {
  min-height: calc(100vh - 140px);
  margin: -22px -26px -24px;
  padding: 20px 26px 32px;
  background: #f0f2f5;
  box-sizing: border-box;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
  margin-bottom: 16px;
}

.panel-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 20px 24px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.02em;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.link-excel {
  font-size: 14px;
  font-weight: 500;
}

.btn-new {
  min-width: 120px;
  font-weight: 600;
  border-radius: 8px;
}

.panel-search {
  padding: 18px 24px 20px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 12px;
}

.filter-row-selects {
  margin-bottom: 14px;
}

.f-course {
  min-width: 220px;
  max-width: 100%;
  flex: 1 1 200px;
}

.f-kp {
  min-width: 180px;
  flex: 1 1 160px;
}

.f-type {
  min-width: 140px;
  flex: 0 1 140px;
}

.filter-row-keyword {
  align-items: stretch;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px 0 0 8px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: box-shadow 0.2s;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

.search-btn {
  border-radius: 0 8px 8px 0;
  margin-left: -1px;
  padding-left: 28px;
  padding-right: 28px;
  font-weight: 600;
}

.panel-table {
  padding: 0 0 16px;
  overflow: hidden;
}

.data-table {
  width: 100%;
}

.data-table :deep(.el-table__header-wrapper th.el-table__cell) {
  border-bottom: 1px solid #ebeef5;
}

.data-table :deep(.el-table__body td.el-table__cell) {
  border-bottom: 1px solid #ebeef5;
}

.table-empty {
  padding: 48px 0;
  color: #909399;
  font-size: 14px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px 0;
}

.hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
.dedup-table {
  margin-top: 14px;
}
.dedup-meta {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}
.batch-hint {
  margin: 0 0 12px;
  color: #606266;
  font-size: 13px;
}
.ver-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
