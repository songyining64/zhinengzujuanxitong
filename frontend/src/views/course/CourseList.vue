<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索课程名" clearable style="width: 220px" @keyup.enter="fetchData" />
      <el-button type="primary" style="margin-left: 8px" @click="fetchData">搜索</el-button>
      <el-button v-if="canManage" type="primary" style="margin-left: auto" @click="openCreate">新建课程</el-button>
    </div>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="code" label="编码" width="120" />
      <el-table-column prop="description" label="简介" min-width="160" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80" />
      <el-table-column v-if="canManage" label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openStudents(row)">学生</el-button>
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

    <el-dialog v-model="formVisible" :title="editingId ? '编辑课程' : '新建课程'" width="480px" destroy-on-close>
      <el-form :model="form" label-width="88px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item v-if="editingId" label="状态">
          <el-input-number v-model="form.status" :min="0" :max="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="studentDrawer" title="课程学生" size="420px" destroy-on-close>
      <div v-if="currentCourse" class="drawer-body">
        <el-form inline @submit.prevent>
          <el-form-item label="学生用户ID">
            <el-input-number v-model="addStudentId" :min="1" controls-position="right" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="addingStudent" @click="addStudent">加入课程</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="students" size="small" stripe>
          <el-table-column prop="studentId" label="用户ID" width="88" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column label="操作" width="72">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeStudent(row.studentId)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue';
import http from '@/api/http';

interface CourseRow {
  id: number;
  name: string;
  code?: string;
  description?: string;
  status?: number;
}

interface CourseStudentRow {
  studentId: number;
  username: string;
  realName?: string;
}

const role = () => localStorage.getItem('role') || '';
const canManage = computed(() => role() === 'ADMIN' || role() === 'TEACHER');

const keyword = ref('');
const page = ref(1);
const size = ref(10);
const total = ref(0);
const list = ref<CourseRow[]>([]);
const loading = ref(false);

const formVisible = ref(false);
const editingId = ref<number | null>(null);
const form = reactive({ name: '', code: '', description: '', status: 1 });
const saving = ref(false);

const studentDrawer = ref(false);
const currentCourse = ref<CourseRow | null>(null);
const students = ref<CourseStudentRow[]>([]);
const addStudentId = ref<number | undefined>(undefined);
const addingStudent = ref(false);

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await http.get<{ records: CourseRow[]; total: number }>('/api/course', {
      params: { keyword: keyword.value || undefined, page: page.value, size: size.value }
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  editingId.value = null;
  form.name = '';
  form.code = '';
  form.description = '';
  form.status = 1;
  formVisible.value = true;
};

const openEdit = (row: CourseRow) => {
  editingId.value = row.id;
  form.name = row.name;
  form.code = row.code || '';
  form.description = row.description || '';
  form.status = row.status ?? 1;
  formVisible.value = true;
};

const saveCourse = async () => {
  if (!form.name.trim()) return;
  saving.value = true;
  try {
    if (editingId.value) {
      await http.put(`/api/course/${editingId.value}`, {
        name: form.name,
        code: form.code || undefined,
        description: form.description || undefined,
        status: form.status
      });
    } else {
      await http.post('/api/course', {
        name: form.name,
        code: form.code || undefined,
        description: form.description || undefined
      });
    }
    formVisible.value = false;
    await fetchData();
  } finally {
    saving.value = false;
  }
};

const loadStudents = async () => {
  if (!currentCourse.value) return;
  const { data } = await http.get<CourseStudentRow[]>(`/api/course/${currentCourse.value.id}/students`);
  students.value = data ?? [];
};

const openStudents = async (row: CourseRow) => {
  currentCourse.value = row;
  studentDrawer.value = true;
  await loadStudents();
};

const addStudent = async () => {
  if (!currentCourse.value || !addStudentId.value) return;
  addingStudent.value = true;
  try {
    await http.post(`/api/course/${currentCourse.value.id}/students/${addStudentId.value}`);
    addStudentId.value = undefined;
    await loadStudents();
  } finally {
    addingStudent.value = false;
  }
};

const removeStudent = async (studentId: number) => {
  if (!currentCourse.value) return;
  await http.delete(`/api/course/${currentCourse.value.id}/students/${studentId}`);
  await loadStudents();
};

onMounted(fetchData);
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.drawer-body {
  padding: 0 8px;
}
</style>
