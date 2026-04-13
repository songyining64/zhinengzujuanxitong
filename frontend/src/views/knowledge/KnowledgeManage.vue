<template>
  <el-card>
    <template #header>知识点管理</template>
    <div class="bar">
      <span>课程</span>
      <el-select v-model="courseId" placeholder="选择课程" filterable style="width: 280px" @change="onCourseChange">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" :disabled="!courseId" @click="openCreateRoot">新增根节点</el-button>
    </div>

    <el-tree
      v-if="courseId"
      v-loading="loading"
      :data="tree"
      :props="{ label: 'label', children: 'children' }"
      default-expand-all
      node-key="id"
      highlight-current
    >
      <template #default="{ data }">
        <span class="tree-node">
          <span>{{ data.label }}</span>
          <span class="acts">
            <el-button type="primary" link size="small" @click.stop="openCreateChild(data.raw)">子节点</el-button>
            <el-button type="primary" link size="small" @click.stop="openEdit(data.raw)">编辑</el-button>
            <el-button type="danger" link size="small" @click.stop="onDelete(data.raw)">删除</el-button>
          </span>
        </span>
      </template>
    </el-tree>
    <el-empty v-else description="请选择课程" />

    <el-dialog v-model="dlg" :title="dlgTitle" width="420px" destroy-on-close @closed="resetDlg">
      <el-form :model="form" label-width="88px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveKp">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import { fetchKnowledgeList, createKnowledge, updateKnowledge, deleteKnowledge, type KpRow } from '@/api/knowledge';
import { getLastCourseId, setLastCourseId } from '@/composables/useLastCourseId';

interface TreeNode {
  id: number;
  label: string;
  raw: KpRow;
  children?: TreeNode[];
}

const loading = ref(false);
const courses = ref<CourseRow[]>([]);
const courseId = ref<number | null>(getLastCourseId());
const tree = ref<TreeNode[]>([]);

const dlg = ref(false);
const dlgTitle = ref('');
const saving = ref(false);
const editingId = ref<number | null>(null);
const parentForCreate = ref<number | null | undefined>(undefined);
const form = reactive({ name: '', sortOrder: 0 });

function buildTree(rows: KpRow[]): TreeNode[] {
  const map = new Map<number, TreeNode>();
  rows.forEach((r) => {
    map.set(r.id, { id: r.id, label: r.name, raw: r, children: [] });
  });
  const roots: TreeNode[] = [];
  rows.forEach((r) => {
    const n = map.get(r.id)!;
    if (r.parentId == null || !map.has(r.parentId)) {
      roots.push(n);
    } else {
      map.get(r.parentId)!.children!.push(n);
    }
  });
  return roots;
}

async function loadCourses() {
  const { data } = await fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
  }
}

async function load() {
  if (!courseId.value) {
    tree.value = [];
    return;
  }
  setLastCourseId(courseId.value);
  loading.value = true;
  try {
    const { data } = await fetchKnowledgeList(courseId.value);
    tree.value = buildTree(data ?? []);
  } finally {
    loading.value = false;
  }
}

function onCourseChange() {
  void load();
}

function openCreateRoot() {
  editingId.value = null;
  parentForCreate.value = null;
  dlgTitle.value = '新增根知识点';
  form.name = '';
  form.sortOrder = 0;
  dlg.value = true;
}

function openCreateChild(row: KpRow) {
  editingId.value = null;
  parentForCreate.value = row.id;
  dlgTitle.value = '新增子知识点';
  form.name = '';
  form.sortOrder = 0;
  dlg.value = true;
}

function openEdit(row: KpRow) {
  editingId.value = row.id;
  parentForCreate.value = undefined;
  dlgTitle.value = '编辑知识点';
  form.name = row.name;
  form.sortOrder = row.sortOrder ?? 0;
  dlg.value = true;
}

function resetDlg() {
  editingId.value = null;
  parentForCreate.value = undefined;
}

async function saveKp() {
  if (!courseId.value || !form.name.trim()) return;
  saving.value = true;
  try {
    if (editingId.value) {
      await updateKnowledge(editingId.value, { name: form.name, sortOrder: form.sortOrder });
    } else {
      await createKnowledge({
        courseId: courseId.value,
        parentId: parentForCreate.value ?? undefined,
        name: form.name,
        sortOrder: form.sortOrder
      });
    }
    dlg.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function onDelete(row: KpRow) {
  try {
    await ElMessageBox.confirm('将删除该节点及其子节点，确定吗？', '删除', { type: 'warning' });
  } catch {
    return;
  }
  await deleteKnowledge(row.id);
  await load();
}

onMounted(async () => {
  await loadCourses();
  await load();
});
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
  padding-right: 8px;
}
.acts {
  display: inline-flex;
  gap: 4px;
}
</style>
