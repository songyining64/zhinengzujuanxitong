<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">试卷管理</span>
        <div class="header-actions">
          <span class="label">课程</span>
          <el-select
            v-model="selectedCourseId"
            placeholder="请选择课程"
            clearable
            filterable
            style="width: 260px"
            @change="onCourseChange"
          >
            <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
          </el-select>
        </div>
      </div>
    </template>

    <el-tabs v-model="tab">
      <el-tab-pane label="自动组卷" name="auto">
        <el-form :model="autoForm" label-width="132px" class="section-form">
          <el-form-item label="试卷标题">
            <el-input v-model="autoForm.title" placeholder="例如：高一数学月考卷A" style="max-width: 520px" />
          </el-form-item>
          <el-form-item label="随机全课程抽题">
            <el-switch v-model="autoForm.randomPool" />
          </el-form-item>
          <el-form-item label="知识点（非随机时）" v-if="!autoForm.randomPool">
            <el-tree-select
              v-model="autoForm.knowledgePointIds"
              :data="kpTree"
              :props="{ label: 'name', children: 'children', value: 'id' }"
              multiple
              clearable
              filterable
              check-strictly
              placeholder="可多选"
              style="max-width: 520px"
            />
          </el-form-item>
          <el-form-item label="每题分值">
            <el-input-number v-model="autoForm.scorePerQuestion" :min="1" :max="100" />
          </el-form-item>
          <el-form-item label="随机种子（可选）">
            <el-input-number v-model="autoForm.randomSeed" :min="1" :max="2147483647" />
          </el-form-item>
          <el-form-item label="题型数量配置">
            <div class="type-grid">
              <div class="type-item">
                <span class="type-name">单选题</span>
                <el-input-number v-model="autoForm.countByType.SINGLE" :min="0" :max="100" />
              </div>
              <div class="type-item">
                <span class="type-name">多选题</span>
                <el-input-number v-model="autoForm.countByType.MULTIPLE" :min="0" :max="100" />
              </div>
              <div class="type-item">
                <span class="type-name">判断题</span>
                <el-input-number v-model="autoForm.countByType.TRUE_FALSE" :min="0" :max="100" />
              </div>
              <div class="type-item">
                <span class="type-name">填空题</span>
                <el-input-number v-model="autoForm.countByType.FILL" :min="0" :max="100" />
              </div>
              <div class="type-item">
                <span class="type-name">简答题</span>
                <el-input-number v-model="autoForm.countByType.SHORT" :min="0" :max="100" />
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="!selectedCourseId" :loading="autoSubmitting" @click="onAutoGenerate">
              生成试卷
            </el-button>
            <el-button :disabled="!selectedCourseId" @click="openSaveTemplate">保存为模板</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="组卷模板" name="template">
        <div class="toolbar">
          <el-button type="primary" :disabled="!selectedCourseId" @click="openSaveTemplate">新增模板</el-button>
          <el-button :disabled="!selectedCourseId" @click="fetchTemplates">刷新</el-button>
        </div>
        <el-table :data="templateList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="模板名称" min-width="180" />
          <el-table-column prop="updateTime" label="更新时间" width="170" />
          <el-table-column label="操作" width="260">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditTemplate(row)">编辑</el-button>
              <el-button link type="primary" @click="openGenerateFromTemplate(row)">按模板组卷</el-button>
              <el-button link type="danger" @click="onDeleteTemplate(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="试卷列表" name="paper">
        <div class="toolbar">
          <el-button :disabled="!selectedCourseId" @click="fetchPapers">刷新</el-button>
        </div>
        <el-table :data="paperList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="试卷标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="mode" label="模式" width="100">
            <template #default="{ row }">{{ modeLabel(row.mode) }}</template>
          </el-table-column>
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPaperDetail(row)">详情</el-button>
              <el-button link type="danger" @click="onDeletePaper(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="paperPage"
            v-model:page-size="paperSize"
            :total="paperTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="fetchPapers"
            @size-change="fetchPapers"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="生成日志" name="log">
        <el-table :data="logList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="paperId" label="试卷ID" width="90" />
          <el-table-column prop="mode" label="模式" width="90">
            <template #default="{ row }">{{ modeLabel(row.mode) }}</template>
          </el-table-column>
          <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
          <el-table-column prop="createTime" label="时间" width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="templateVisible" :title="editingTemplateId ? '编辑模板' : '保存为模板'" width="640px">
      <el-form :model="templateForm" label-width="96px">
        <el-form-item label="模板名称">
          <el-input v-model="templateForm.name" maxlength="128" />
        </el-form-item>
        <el-form-item label="规则JSON">
          <el-input v-model="templateForm.rulesJson" type="textarea" :rows="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateVisible = false">取消</el-button>
        <el-button type="primary" :loading="templateSubmitting" @click="submitTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fromTemplateVisible" title="按模板生成试卷" width="520px">
      <el-form :model="fromTemplateForm" label-width="96px">
        <el-form-item label="试卷标题">
          <el-input v-model="fromTemplateForm.title" />
        </el-form-item>
        <el-form-item label="随机种子">
          <el-input-number v-model="fromTemplateForm.randomSeed" :min="1" :max="2147483647" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fromTemplateVisible = false">取消</el-button>
        <el-button type="primary" :loading="fromTemplateSubmitting" @click="submitGenerateFromTemplate">
          生成
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paperDetailVisible" title="试卷详情" width="760px">
      <el-descriptions v-if="paperDetail" :column="2" border>
        <el-descriptions-item label="试卷ID">{{ paperDetail.paper.id }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ paperDetail.paper.title }}</el-descriptions-item>
        <el-descriptions-item label="模式">{{ modeLabel(paperDetail.paper.mode) }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ paperDetail.paper.totalScore }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="paperDetail" :data="paperDetail.questions" stripe style="margin-top: 12px" max-height="360">
        <el-table-column prop="questionOrder" label="序号" width="70" />
        <el-table-column prop="questionId" label="题目ID" width="90" />
        <el-table-column prop="type" label="题型" width="100" />
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column prop="stem" label="题干" min-width="300" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '@/api/http';
import {
  autoGeneratePaper,
  deletePaper,
  deletePaperTemplate,
  fetchPaperDetail,
  fetchPaperGenerationLogs,
  fetchPaperPage,
  fetchPaperTemplates,
  generatePaperFromTemplate,
  savePaperTemplate,
  updatePaperTemplate,
  type PaperDetailVO,
  type PaperGenerationLog,
  type PaperRow,
  type PaperTemplate
} from '@/api/modules/paper';

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
  children?: KpRow[];
}

const tab = ref('auto');
const selectedCourseId = ref<number | undefined>(undefined);
const courseList = ref<CourseRow[]>([]);
const kpTree = ref<KpRow[]>([]);

const autoForm = reactive({
  title: '',
  randomPool: false,
  knowledgePointIds: [] as number[],
  scorePerQuestion: 2,
  randomSeed: undefined as number | undefined,
  countByType: {
    SINGLE: 0,
    MULTIPLE: 0,
    TRUE_FALSE: 0,
    FILL: 0,
    SHORT: 0
  }
});
const autoSubmitting = ref(false);

const templateList = ref<PaperTemplate[]>([]);
const templateVisible = ref(false);
const templateSubmitting = ref(false);
const editingTemplateId = ref<number | null>(null);
const templateForm = reactive({
  name: '',
  rulesJson: '{}'
});

const fromTemplateVisible = ref(false);
const fromTemplateSubmitting = ref(false);
const selectedTemplateId = ref<number | null>(null);
const fromTemplateForm = reactive({
  title: '',
  randomSeed: undefined as number | undefined
});

const paperList = ref<PaperRow[]>([]);
const paperPage = ref(1);
const paperSize = ref(10);
const paperTotal = ref(0);

const logList = ref<PaperGenerationLog[]>([]);

const paperDetailVisible = ref(false);
const paperDetail = ref<PaperDetailVO | null>(null);

function courseLabel(c: CourseRow) {
  return c.code ? `${c.name}（${c.code}）` : c.name;
}
function modeLabel(v?: string) {
  if (v === 'AUTO') return '自动';
  if (v === 'TEMPLATE') return '模板';
  if (v === 'MANUAL') return '手动';
  return v || '-';
}

function buildKpTree(items: KpRow[]): KpRow[] {
  const map = new Map<number, KpRow>();
  items.forEach((i) => map.set(i.id, { ...i, children: [] }));
  const roots: KpRow[] = [];
  for (const i of items) {
    const node = map.get(i.id)!;
    const pid = i.parentId;
    if (pid == null) roots.push(node);
    else map.get(pid)?.children?.push(node);
  }
  return roots;
}

async function fetchCourses() {
  const { data } = await http.get<CoursePage>('/api/course', { params: { page: 1, size: 200 } });
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
  const { data } = await http.get<KpRow[]>('/api/knowledge-point', { params: { courseId: selectedCourseId.value } });
  const list = (data ?? []).map((k) => ({ ...k, parentId: k.parentId ?? null }));
  kpTree.value = buildKpTree(list);
}

async function fetchTemplates() {
  if (!selectedCourseId.value) {
    templateList.value = [];
    return;
  }
  templateList.value = await fetchPaperTemplates(selectedCourseId.value);
}

async function fetchPapers() {
  if (!selectedCourseId.value) {
    paperList.value = [];
    paperTotal.value = 0;
    return;
  }
  const page = await fetchPaperPage({ courseId: selectedCourseId.value, page: paperPage.value, size: paperSize.value });
  paperList.value = page?.records ?? [];
  paperTotal.value = page?.total ?? 0;
}

async function fetchLogs() {
  if (!selectedCourseId.value) {
    logList.value = [];
    return;
  }
  const page = await fetchPaperGenerationLogs({ courseId: selectedCourseId.value, page: 1, size: 20 });
  logList.value = page?.records ?? [];
}

async function onCourseChange() {
  paperPage.value = 1;
  await fetchKnowledgePoints();
  await Promise.all([fetchTemplates(), fetchPapers(), fetchLogs()]);
}

function toCountByType() {
  const out: Record<string, number> = {};
  for (const [k, v] of Object.entries(autoForm.countByType)) {
    if (v > 0) out[k] = v;
  }
  return out;
}

function rulesFromAutoForm() {
  return {
    courseId: selectedCourseId.value,
    knowledgePointIds: autoForm.randomPool ? undefined : (autoForm.knowledgePointIds.length ? autoForm.knowledgePointIds : undefined),
    randomPool: autoForm.randomPool,
    includeKnowledgeDescendants: true,
    scorePerQuestion: autoForm.scorePerQuestion,
    randomSeed: autoForm.randomSeed,
    dedup: true,
    countByType: toCountByType()
  };
}

async function onAutoGenerate() {
  if (!selectedCourseId.value) return;
  const countByType = toCountByType();
  if (!autoForm.title.trim()) {
    ElMessage.warning('请填写试卷标题');
    return;
  }
  if (!Object.keys(countByType).length) {
    ElMessage.warning('请至少设置一种题型数量');
    return;
  }
  autoSubmitting.value = true;
  try {
    await autoGeneratePaper({
      courseId: selectedCourseId.value,
      title: autoForm.title.trim(),
      knowledgePointIds: autoForm.randomPool ? undefined : autoForm.knowledgePointIds,
      randomPool: autoForm.randomPool,
      scorePerQuestion: autoForm.scorePerQuestion,
      randomSeed: autoForm.randomSeed,
      includeKnowledgeDescendants: true,
      dedup: true,
      countByType
    });
    ElMessage.success('试卷生成成功');
    await Promise.all([fetchPapers(), fetchLogs()]);
  } finally {
    autoSubmitting.value = false;
  }
}

function openSaveTemplate() {
  editingTemplateId.value = null;
  templateForm.name = '';
  templateForm.rulesJson = JSON.stringify(rulesFromAutoForm(), null, 2);
  templateVisible.value = true;
}

function openEditTemplate(row: PaperTemplate) {
  editingTemplateId.value = row.id;
  templateForm.name = row.name;
  templateForm.rulesJson = row.rulesJson || '{}';
  templateVisible.value = true;
}

async function submitTemplate() {
  if (!selectedCourseId.value) return;
  if (!templateForm.name.trim()) {
    ElMessage.warning('请输入模板名称');
    return;
  }
  let rules: Record<string, unknown>;
  try {
    rules = JSON.parse(templateForm.rulesJson || '{}');
  } catch {
    ElMessage.warning('规则JSON格式错误');
    return;
  }
  templateSubmitting.value = true;
  try {
    if (editingTemplateId.value) {
      await updatePaperTemplate(editingTemplateId.value, {
        courseId: selectedCourseId.value,
        name: templateForm.name.trim(),
        rules
      });
      ElMessage.success('模板更新成功');
    } else {
      await savePaperTemplate({
        courseId: selectedCourseId.value,
        name: templateForm.name.trim(),
        rules
      });
      ElMessage.success('模板保存成功');
    }
    templateVisible.value = false;
    await fetchTemplates();
  } finally {
    templateSubmitting.value = false;
  }
}

function openGenerateFromTemplate(row: PaperTemplate) {
  selectedTemplateId.value = row.id;
  fromTemplateForm.title = `${row.name}-试卷`;
  fromTemplateForm.randomSeed = undefined;
  fromTemplateVisible.value = true;
}

async function submitGenerateFromTemplate() {
  if (!selectedTemplateId.value) return;
  if (!fromTemplateForm.title.trim()) {
    ElMessage.warning('请输入试卷标题');
    return;
  }
  fromTemplateSubmitting.value = true;
  try {
    await generatePaperFromTemplate(selectedTemplateId.value, {
      title: fromTemplateForm.title.trim(),
      randomSeed: fromTemplateForm.randomSeed
    });
    ElMessage.success('已按模板生成试卷');
    fromTemplateVisible.value = false;
    await Promise.all([fetchPapers(), fetchLogs()]);
  } finally {
    fromTemplateSubmitting.value = false;
  }
}

async function onDeleteTemplate(row: PaperTemplate) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${row.name}」吗？`, '删除模板', { type: 'warning' });
  } catch {
    return;
  }
  await deletePaperTemplate(row.id);
  ElMessage.success('模板已删除');
  await fetchTemplates();
}

async function openPaperDetail(row: PaperRow) {
  paperDetail.value = await fetchPaperDetail(row.id);
  paperDetailVisible.value = true;
}

async function onDeletePaper(row: PaperRow) {
  try {
    await ElMessageBox.confirm(`确定删除试卷「${row.title}」吗？`, '删除试卷', { type: 'warning' });
  } catch {
    return;
  }
  await deletePaper(row.id);
  ElMessage.success('试卷已删除');
  await fetchPapers();
}

onMounted(async () => {
  await fetchCourses();
  await fetchKnowledgePoints();
  await Promise.all([fetchTemplates(), fetchPapers(), fetchLogs()]);
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
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.label {
  font-size: 13px;
  color: #606266;
}
.section-form {
  max-width: 920px;
}
.type-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 10px 16px;
  width: 100%;
  max-width: 560px;
}
.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.type-name {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
.toolbar {
  margin-bottom: 10px;
  display: flex;
  gap: 8px;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>

