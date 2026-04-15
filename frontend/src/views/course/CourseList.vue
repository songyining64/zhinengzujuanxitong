<template>
  <div class="course-split">
    <aside class="split-left">
      <el-card shadow="never" class="tree-card">
        <template #header>
          <span>学科知识结构</span>
          <el-tag v-if="treeCourseId" size="small" type="info">课程 {{ treeCourseId }}</el-tag>
        </template>
        <p v-if="!treeCourseId" class="muted">请在右侧表格操作列点击「知识」或「专题」，将自动切换当前课程并跳转；也可点击表格行在下方编辑。</p>
        <el-tree
          v-else
          v-loading="treeLoading"
          :data="kpTree"
          :props="{ label: 'label', children: 'children' }"
          default-expand-all
          highlight-current
          class="kp-tree"
        >
          <template #default="{ data }">
            <div class="kp-tree-node">
              <span class="kp-tree-name">{{ data.label }}</span>
              <p v-if="data.content" class="kp-tree-content">{{ data.content }}</p>
            </div>
          </template>
        </el-tree>
        <div v-if="treeCourseId" class="tree-hint">知识点维护也可在「知识点」页拖拽排序（规划中）</div>
      </el-card>
    </aside>

    <section class="split-right">
      <el-card shadow="never">
        <template #header>
          <div class="head">
            <span>课程信息</span>
            <div class="head-btns">
              <el-button @click="openPanelCreate">右侧面板新建</el-button>
              <el-button type="primary" @click="openCreate">弹窗新建</el-button>
            </div>
          </div>
        </template>

        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="搜索课程名/编号"
            clearable
            style="width: 240px"
            @keyup.enter="fetchData"
          />
          <el-button type="primary" @click="fetchData">搜索</el-button>
        </div>

        <el-alert
          class="sample-alert"
          type="info"
          show-icon
          :closable="false"
          title="列表顶部已预置语文、数学、英语、物理、美术、化学六门学科示例（含简介），便于演示与备课；正式排课请在后端创建真实课程。"
        />

        <el-table
          v-loading="loading"
          :data="list"
          stripe
          highlight-current-row
          @current-change="onRowSelect"
        >
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column label="课程名称" min-width="160">
            <template #default="{ row }">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isSample" size="small" type="info" class="name-tag">示例</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="编号" width="120" />
          <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goKnowledge(row)">知识</el-button>
              <el-button link type="primary" @click="goTopic(row)">专题</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="panel.id != null || panel.create" shadow="never" class="edit-panel">
        <template #header>
          <span>{{ panel.create ? '新建课程' : `编辑：${panel.name || panel.id}` }}</span>
        </template>
        <el-form :model="panel.form" label-width="88px" class="panel-form">
          <el-form-item label="名称" required>
            <el-input v-model="panel.form.name" placeholder="课程名称" />
          </el-form-item>
          <el-form-item label="编号">
            <el-input v-model="panel.form.code" placeholder="可选" />
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="panel.form.description" type="textarea" rows="4" placeholder="课程简介" />
          </el-form-item>
          <el-form-item>
            <el-alert
              v-if="!panel.create && panel.id && isSampleCourseId(panel.id)"
              type="warning"
              :closable="false"
              show-icon
              class="panel-warn"
              title="当前为内置示例学科，保存不会写入后端；正式使用请「弹窗新建」或连接后端创建课程。"
            />
            <el-button
              type="primary"
              :loading="panel.saving"
              :disabled="!panel.create && !!panel.id && isSampleCourseId(panel.id)"
              @click="savePanel"
            >
              保存
            </el-button>
            <el-button
              v-if="!panel.create && panel.id && !isSampleCourseId(panel.id)"
              @click="openStudents(panel.id)"
            >
              管理学生
            </el-button>
            <el-button
              v-if="!panel.create && panel.id"
              @click="useAsContext({ id: panel.id, name: panel.form.name || panel.name })"
            >
              设为当前课程
            </el-button>
            <el-button @click="closePanel">关闭</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>

    <el-dialog v-model="dlg.visible" :title="dlg.editingId ? '编辑课程' : '新建课程'" width="480px" destroy-on-close>
      <el-form :model="dlg.form" label-width="88px">
        <el-form-item label="名称" required>
          <el-input v-model="dlg.form.name" placeholder="课程名称" />
        </el-form-item>
        <el-form-item label="编号">
          <el-input v-model="dlg.form.code" placeholder="可选" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="dlg.form.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="saveCourseDialog">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stuDlg.visible" title="课程学生" width="560px" destroy-on-close @open="loadStudents">
      <div class="stu-bar">
        <el-input-number v-model="stuDlg.newStudentId" :min="1" controls-position="right" />
        <el-button type="primary" :loading="stuDlg.adding" @click="addStudent">添加学生</el-button>
      </div>
      <el-table :data="stuDlg.list" size="small" max-height="360">
        <el-table-column prop="studentId" label="学生ID" width="100" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column width="90">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeStudent(row.studentId)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  fetchCoursePage,
  createCourse,
  updateCourse,
  fetchCourseStudents,
  addCourseStudent,
  removeCourseStudent
} from '@/api/modules/course';
import { fetchKnowledgeList } from '@/api/modules/knowledge';
import { useCourseContextStore } from '@/store/courseContext';
import { buildKnowledgeTree } from '@/utils/knowledgeTree';
import type { Course } from '@/types/models';
import type { TreeNode } from '@/utils/knowledgeTree';
import { SAMPLE_SUBJECT_COURSES, isSampleCourseId } from '@/data/sampleCourses';
import { resolveKnowledgePointsForCourse } from '@/data/sampleKnowledgeByCourse';

type CourseRow = Course & { isSample?: boolean };

const loading = ref(false);
const treeLoading = ref(false);
const keyword = ref('');
const list = ref<CourseRow[]>([]);
const treeCourseId = ref<number | null>(null);
const kpTree = ref<TreeNode[]>([]);

const dlg = reactive({
  visible: false,
  editingId: null as number | null,
  saving: false,
  form: { name: '', code: '', description: '' }
});

const panel = reactive({
  create: false,
  id: null as number | null,
  name: '' as string,
  saving: false,
  form: { name: '', code: '', description: '' }
});

const stuDlg = reactive({
  visible: false,
  courseId: null as number | null,
  list: [] as { studentId: number; username: string; realName?: string }[],
  newStudentId: undefined as number | undefined,
  adding: false
});

const router = useRouter();
const courseStore = useCourseContextStore();

function goKnowledge(row: CourseRow) {
  courseStore.setCourseId(row.id);
  treeCourseId.value = row.id;
  void router.push('/knowledge');
}

/** 专题 → 题库（按课程筛选试题） */
function goTopic(row: CourseRow) {
  courseStore.setCourseId(row.id);
  treeCourseId.value = row.id;
  void router.push('/question');
}

async function loadTree(courseId: number | null) {
  if (!courseId) {
    kpTree.value = [];
    return;
  }
  treeLoading.value = true;
  try {
    let list;
    try {
      const { data } = await fetchKnowledgeList(courseId);
      list = resolveKnowledgePointsForCourse(courseId, data ?? []);
    } catch {
      list = resolveKnowledgePointsForCourse(courseId, []);
    }
    kpTree.value = buildKnowledgeTree(list);
  } finally {
    treeLoading.value = false;
  }
}

watch(treeCourseId, (id) => void loadTree(id));

function matchKeyword(c: Course, kw: string) {
  if (!kw) return true;
  return (
    c.name.includes(kw) ||
    (c.code?.includes(kw) ?? false) ||
    (c.description?.includes(kw) ?? false)
  );
}

async function fetchData() {
  loading.value = true;
  try {
    const kw = keyword.value?.trim() ?? '';
    let apiRows: Course[] = [];
    try {
      const { data } = await fetchCoursePage(1, 50, kw || undefined);
      apiRows = data?.records ?? [];
    } catch {
      apiRows = [];
    }
    const samples = SAMPLE_SUBJECT_COURSES.filter((c) => matchKeyword(c, kw));
    list.value = [...samples, ...apiRows.filter((c) => matchKeyword(c, kw))];
  } finally {
    loading.value = false;
  }
}

function selectRow(row: CourseRow) {
  treeCourseId.value = row.id;
  panel.create = false;
  panel.id = row.id;
  panel.name = row.name;
  panel.form = {
    name: row.name,
    code: row.code || '',
    description: row.description || ''
  };
}

function onRowSelect(row: CourseRow | undefined) {
  if (row) selectRow(row);
}

function openPanelCreate() {
  panel.create = true;
  panel.id = null;
  panel.name = '';
  panel.form = { name: '', code: '', description: '' };
  treeCourseId.value = null;
}

function openCreate() {
  dlg.editingId = null;
  dlg.form = { name: '', code: '', description: '' };
  dlg.visible = true;
}

function closePanel() {
  panel.id = null;
  panel.create = false;
  panel.name = '';
}

async function savePanel() {
  if (!panel.form.name?.trim()) {
    ElMessage.warning('请填写课程名称');
    return;
  }
  if (!panel.create && panel.id != null && isSampleCourseId(panel.id)) {
    ElMessage.info('内置示例学科仅供展示，请在后端创建正式课程后再保存。');
    return;
  }
  panel.saving = true;
  try {
    if (panel.create) {
      const { data } = await createCourse(panel.form);
      ElMessage.success('已创建');
      panel.create = false;
      panel.id = data.id;
      panel.name = data.name;
      treeCourseId.value = data.id;
      courseStore.setCourseId(data.id);
    } else if (panel.id) {
      await updateCourse(panel.id, panel.form);
      ElMessage.success('已更新');
    }
    await fetchData();
  } finally {
    panel.saving = false;
  }
}

async function saveCourseDialog() {
  if (!dlg.form.name?.trim()) {
    ElMessage.warning('请填写课程名称');
    return;
  }
  dlg.saving = true;
  try {
    if (dlg.editingId) {
      await updateCourse(dlg.editingId, dlg.form);
      ElMessage.success('已更新');
    } else {
      await createCourse(dlg.form);
      ElMessage.success('已创建');
    }
    dlg.visible = false;
    await fetchData();
  } finally {
    dlg.saving = false;
  }
}

function useAsContext(row: CourseRow) {
  courseStore.setCourseId(row.id);
  treeCourseId.value = row.id;
  selectRow(row);
  ElMessage.success(`已切换到「${row.name}」`);
}

function openStudents(cid: number) {
  if (isSampleCourseId(cid)) {
    ElMessage.info('示例课程无学生名单，请选用后端真实课程后再管理学生。');
    return;
  }
  stuDlg.courseId = cid;
  stuDlg.visible = true;
}

async function loadStudents() {
  if (!stuDlg.courseId) return;
  const { data } = await fetchCourseStudents(stuDlg.courseId);
  stuDlg.list = data ?? [];
}

async function addStudent() {
  if (!stuDlg.courseId || !stuDlg.newStudentId) {
    ElMessage.warning('请输入学生用户 ID');
    return;
  }
  stuDlg.adding = true;
  try {
    await addCourseStudent(stuDlg.courseId, stuDlg.newStudentId);
    ElMessage.success('已添加');
    stuDlg.newStudentId = undefined;
    await loadStudents();
  } finally {
    stuDlg.adding = false;
  }
}

async function removeStudent(studentId: number) {
  if (!stuDlg.courseId) return;
  await removeCourseStudent(stuDlg.courseId, studentId);
  ElMessage.success('已移除');
  await loadStudents();
}

onMounted(async () => {
  courseStore.loadFromStorage();
  await fetchData();
  const ctx = courseStore.courseId;
  if (ctx) {
    treeCourseId.value = ctx;
    const hit = list.value.find((c) => c.id === ctx);
    if (hit) selectRow(hit);
  }
});
</script>

<style scoped>
.course-split {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.split-left {
  flex: 0 0 300px;
  width: 100%;
  max-width: 360px;
}

.split-right {
  flex: 1;
  min-width: 280px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tree-card {
  min-height: 420px;
}

.kp-tree {
  max-height: 520px;
  overflow: auto;
}

/* 自定义节点含多行说明时，需取消 el-tree 默认单行固定高度，否则会层层叠在一起 */
.kp-tree :deep(.el-tree-node__content) {
  height: auto;
  min-height: 32px;
  align-items: flex-start;
  padding: 6px 0;
  line-height: 1.45;
  white-space: normal;
}

.kp-tree :deep(.el-tree-node__expand-icon) {
  margin-top: 4px;
  flex-shrink: 0;
}

.kp-tree-node {
  display: block;
  max-width: 100%;
  min-width: 0;
  padding-right: 8px;
  white-space: normal;
  word-break: break-word;
}

.kp-tree-name {
  display: block;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.kp-tree-content {
  display: block;
  margin: 6px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.muted {
  color: #909399;
  font-size: 13px;
  margin: 0;
}

.tree-hint {
  margin-top: 12px;
  font-size: 12px;
  color: #c0c4cc;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.head-btns {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.sample-alert {
  margin-bottom: 12px;
}

.name-tag {
  margin-left: 8px;
  vertical-align: middle;
}

.panel-warn {
  margin-bottom: 12px;
}

.edit-panel {
  border: 1px dashed var(--el-border-color);
}

.panel-form {
  max-width: 560px;
}

.stu-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}

@media (max-width: 768px) {
  .split-left {
    flex: 1 1 100%;
    max-width: none;
  }
}
</style>
