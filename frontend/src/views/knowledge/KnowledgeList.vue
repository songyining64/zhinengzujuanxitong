<template>
  <el-card>
    <div class="toolbar">
      <span class="label">课程</span>
      <el-select
        v-model="courseId"
        placeholder="请选择课程"
        filterable
        style="width: 260px"
        @change="fetchList"
      >
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button v-if="canManage && courseId" type="primary" style="margin-left: auto" @click="openCreate">
        新建知识点
      </el-button>
    </div>

    <el-empty v-if="!courseId" description="请先选择课程" />
    <el-table v-else v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="parentId" label="父节点ID" width="100" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column v-if="canManage" label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑知识点' : '新建知识点'" width="440px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="父节点ID">
          <el-input-number v-model="form.parentId" :min="0" controls-position="right" />
          <span class="hint">0 表示无父级，可留空</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import http from '@/api/http';

interface CourseBrief {
  id: number;
  name: string;
}

interface KpRow {
  id: number;
  courseId: number;
  name: string;
  parentId?: number | null;
  sortOrder?: number | null;
}

const role = () => localStorage.getItem('role') || '';
const canManage = computed(() => role() === 'ADMIN' || role() === 'TEACHER');

const courses = ref<CourseBrief[]>([]);
const courseId = ref<number | undefined>(undefined);
const list = ref<KpRow[]>([]);
const loading = ref(false);

const formVisible = ref(false);
const editingId = ref<number | null>(null);
const form = reactive<{ name: string; parentId: number | undefined; sortOrder: number | undefined }>({
  name: '',
  parentId: undefined,
  sortOrder: 0
});
const saving = ref(false);

const loadCourses = async () => {
  const { data } = await http.get<{ records: CourseBrief[] }>('/api/course', { params: { page: 1, size: 500 } });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
    await fetchList();
  }
};

const fetchList = async () => {
  if (!courseId.value) {
    list.value = [];
    return;
  }
  loading.value = true;
  try {
    const { data } = await http.get<KpRow[]>('/api/knowledge-point', { params: { courseId: courseId.value } });
    list.value = data ?? [];
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  editingId.value = null;
  form.name = '';
  form.parentId = undefined;
  form.sortOrder = 0;
  formVisible.value = true;
};

const openEdit = (row: KpRow) => {
  editingId.value = row.id;
  form.name = row.name;
  form.parentId = row.parentId ?? undefined;
  form.sortOrder = row.sortOrder ?? 0;
  formVisible.value = true;
};

const save = async () => {
  if (!courseId.value || !form.name.trim()) return;
  saving.value = true;
  try {
    const parentId = form.parentId && form.parentId > 0 ? form.parentId : undefined;
    if (editingId.value) {
      await http.put(`/api/knowledge-point/${editingId.value}`, {
        name: form.name,
        parentId,
        sortOrder: form.sortOrder
      });
    } else {
      await http.post('/api/knowledge-point', {
        courseId: courseId.value,
        name: form.name,
        parentId,
        sortOrder: form.sortOrder
      });
    }
    formVisible.value = false;
    await fetchList();
  } finally {
    saving.value = false;
  }
};

const remove = async (row: KpRow) => {
  await ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', { type: 'warning' });
  await http.delete(`/api/knowledge-point/${row.id}`);
  await fetchList();
};

onMounted(loadCourses);
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.label {
  color: #606266;
  font-size: 14px;
}
.hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
