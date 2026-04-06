<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">知识点管理</span>
        <el-button type="primary" :disabled="!selectedCourseId" @click="openCreateRoot">+ 新增根知识点</el-button>
      </div>
    </template>

    <div class="toolbar">
      <span class="label">课程</span>
      <el-select
        v-model="selectedCourseId"
        placeholder="请选择课程"
        filterable
        clearable
        style="width: 320px"
        @change="onCourseChange"
      >
        <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
      </el-select>
      <el-button type="primary" style="margin-left: 8px" :disabled="!selectedCourseId" @click="fetchKnowledgePoints">刷新</el-button>
    </div>

    <el-alert
      v-if="coursesLoaded && courseList.length === 0"
      type="warning"
      :closable="false"
      show-icon
      class="course-hint"
    >
      <template #default>
        <span v-if="roleIsTeacher">
          教师账号只能选「授课教师」为自己的课程。库里有课但这里为空，一般是课程的授课教师仍是管理员：请管理员在「课程管理 →
          编辑」里把授课教师改成您；或新建课程时由管理员指定您为授课教师。
        </span>
        <span v-else-if="roleIsAdmin">当前没有课程数据，请先在「课程管理」中创建课程。</span>
        <span v-else>暂无可用课程。</span>
      </template>
    </el-alert>

    <el-table
      v-if="selectedCourseId"
      :data="treeData"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCreateChild(row)">添加子级</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="请先选择课程" />

    <el-dialog v-model="createVisible" :title="createTitle" width="480px" destroy-on-close @closed="resetCreate">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="createForm.name" maxlength="128" show-word-limit placeholder="知识点名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="createForm.sortOrder" :min="0" :max="999999" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑知识点" width="480px" destroy-on-close @closed="resetEdit">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="96px">
        <el-form-item label="上级" prop="parentId">
          <el-select v-model="editForm.parentId" placeholder="不选则为根节点" clearable filterable style="width: 100%">
            <el-option
              v-for="opt in editParentOptions"
              :key="opt.id"
              :label="opt.label"
              :value="opt.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="editForm.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="editForm.sortOrder" :min="0" :max="999999" controls-position="right" style="width: 100%" />
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
import { computed, reactive, ref, onMounted } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '@/api/http';

interface CourseRow {
  id: number;
  name: string;
  code?: string;
}

interface CoursePage {
  records: CourseRow[];
  total: number;
}

const coursesLoaded = ref(false);
const roleIsTeacher = computed(() => localStorage.getItem('role') === 'TEACHER');
const roleIsAdmin = computed(() => localStorage.getItem('role') === 'ADMIN');

interface KpRow {
  id: number;
  courseId: number;
  parentId: number | null;
  name: string;
  sortOrder: number;
  createTime?: string;
  children?: KpRow[];
}

const courseList = ref<CourseRow[]>([]);
const selectedCourseId = ref<number | undefined>(undefined);
const flatList = ref<KpRow[]>([]);
const treeData = ref<KpRow[]>([]);

const createVisible = ref(false);
const createTitle = ref('新增知识点');
const createSubmitting = ref(false);
const createFormRef = ref<FormInstance>();
const creatingParentId = ref<number | null>(null);
const createForm = reactive({
  name: '',
  sortOrder: 0
});
const createRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
};

const editVisible = ref(false);
const editSubmitting = ref(false);
const editFormRef = ref<FormInstance>();
const editingId = ref<number | null>(null);
const editForm = reactive({
  parentId: null as number | null,
  name: '',
  sortOrder: 0
});
const editRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
};

const editParentOptions = computed(() => {
  if (editingId.value == null || !flatList.value.length) return [];
  const ban = descendantIdSet(editingId.value, flatList.value);
  ban.add(editingId.value);
  return flatList.value
    .filter((k) => !ban.has(k.id))
    .map((k) => ({
      id: k.id,
      label: `${k.name} (ID:${k.id})`
    }));
});

function courseLabel(c: CourseRow) {
  if (c.code) return `${c.name}（${c.code}）`;
  return c.name;
}

function buildTree(items: KpRow[]): KpRow[] {
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

function descendantIdSet(rootId: number, flat: KpRow[]): Set<number> {
  const byParent = new Map<number | null, number[]>();
  for (const k of flat) {
    const p = k.parentId;
    if (!byParent.has(p)) byParent.set(p, []);
    byParent.get(p)!.push(k.id);
  }
  const out = new Set<number>();
  const stack = [...(byParent.get(rootId) || [])];
  while (stack.length) {
    const id = stack.pop()!;
    out.add(id);
    const next = byParent.get(id) || [];
    stack.push(...next);
  }
  return out;
}

async function fetchCourses() {
  coursesLoaded.value = false;
  try {
    const { data } = await http.get<CoursePage>('/api/course', { params: { page: 1, size: 200 } });
    courseList.value = data?.records ?? [];
  } finally {
    coursesLoaded.value = true;
  }
}

async function fetchKnowledgePoints() {
  if (!selectedCourseId.value) {
    flatList.value = [];
    treeData.value = [];
    return;
  }
  const { data } = await http.get<KpRow[]>('/api/knowledge-point', {
    params: { courseId: selectedCourseId.value }
  });
  const list = (data ?? []).map((k) => ({
    ...k,
    parentId: k.parentId ?? null
  }));
  flatList.value = list;
  treeData.value = buildTree(list);
}

function onCourseChange() {
  fetchKnowledgePoints();
}

function openCreateRoot() {
  creatingParentId.value = null;
  createTitle.value = '新增根知识点';
  resetCreateFields();
  createVisible.value = true;
}

function openCreateChild(row: KpRow) {
  creatingParentId.value = row.id;
  createTitle.value = '添加子知识点';
  resetCreateFields();
  createVisible.value = true;
}

function resetCreateFields() {
  createForm.name = '';
  createForm.sortOrder = 0;
  createFormRef.value?.resetFields();
}

function resetCreate() {
  creatingParentId.value = null;
}

const submitCreate = async () => {
  const el = createFormRef.value;
  if (!el || !selectedCourseId.value) return;
  await el.validate(async (valid) => {
    if (!valid) return;
    createSubmitting.value = true;
    try {
      await http.post('/api/knowledge-point', {
        courseId: selectedCourseId.value,
        parentId: creatingParentId.value ?? undefined,
        name: createForm.name.trim(),
        sortOrder: createForm.sortOrder
      });
      ElMessage.success('创建成功');
      createVisible.value = false;
      await fetchKnowledgePoints();
    } finally {
      createSubmitting.value = false;
    }
  });
};

function openEdit(row: KpRow) {
  editingId.value = row.id;
  editForm.parentId = row.parentId;
  editForm.name = row.name;
  editForm.sortOrder = row.sortOrder;
  editVisible.value = true;
}

function resetEdit() {
  editingId.value = null;
  editFormRef.value?.resetFields();
}

const submitEdit = async () => {
  const el = editFormRef.value;
  if (!el || editingId.value == null) return;
  await el.validate(async (valid) => {
    if (!valid) return;
    editSubmitting.value = true;
    try {
      await http.put(`/api/knowledge-point/${editingId.value}`, {
        parentId: editForm.parentId,
        name: editForm.name.trim(),
        sortOrder: editForm.sortOrder
      });
      ElMessage.success('保存成功');
      editVisible.value = false;
      await fetchKnowledgePoints();
    } finally {
      editSubmitting.value = false;
    }
  });
};

async function confirmDelete(row: KpRow) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」及其所有下级知识点？`,
      '删除知识点',
      { type: 'warning' }
    );
  } catch {
    return;
  }
  await http.delete(`/api/knowledge-point/${row.id}`);
  ElMessage.success('已删除');
  await fetchKnowledgePoints();
}

onMounted(async () => {
  await fetchCourses();
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
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar .label {
  font-size: 14px;
  color: #606266;
}

.course-hint {
  margin-bottom: 16px;
}
</style>
