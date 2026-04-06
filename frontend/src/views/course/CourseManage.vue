<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">课程管理</span>
        <el-button type="primary" @click="openCreate">+ 新增课程</el-button>
      </div>
    </template>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="query.keyword"
          placeholder="搜索课程名称/课程代码"
          style="width: 240px"
          clearable
          @keyup.enter="fetchData"
        />
        <el-button type="primary" style="margin-left: 8px" @click="fetchData">搜索</el-button>
      </div>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="课程名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="code" label="课程代码" width="120" show-overflow-tooltip />
      <el-table-column prop="description" label="简介" min-width="160" show-overflow-tooltip />
      <el-table-column label="授课教师名称" min-width="180">
        <template #default="{ row }">
          {{ row.teacherName || '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="96">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openStudents(row)">选课学生</el-button>
          <el-button
            v-if="isAdmin"
            link
            type="danger"
            @click="onDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </div>

    <el-dialog v-model="createVisible" title="新增课程" width="520px" destroy-on-close @closed="resetCreate">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="createForm.name" maxlength="128" show-word-limit placeholder="必填" />
        </el-form-item>
        <el-form-item label="课程代码" prop="code">
          <el-input v-model="createForm.code" maxlength="64" show-word-limit placeholder="可选" />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="3" maxlength="512" show-word-limit placeholder="可选" />
        </el-form-item>
        <el-form-item v-if="isAdmin" label="授课教师名称" prop="teacherId">
          <el-select
            v-model="createForm.teacherId"
            placeholder="不选则默认为当前管理员"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option v-for="o in instructorOptions" :key="o.id" :label="o.label" :value="o.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑课程" width="520px" destroy-on-close @closed="resetEdit">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="96px">
        <el-form-item v-if="isAdmin" label="授课教师名称" prop="teacherId">
          <el-select v-model="editForm.teacherId" placeholder="请选择" filterable style="width: 100%">
            <el-option v-for="o in instructorOptions" :key="o.id" :label="o.label" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="editForm.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="课程代码" prop="code">
          <el-input v-model="editForm.code" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="3" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="studentsVisible" :title="studentsTitle" width="640px" destroy-on-close @closed="resetStudents">
      <div class="student-toolbar">
        <el-input
          v-model="studentIdInput"
          placeholder="学生用户 ID（数字）"
          style="width: 200px"
          clearable
          @keyup.enter="addStudent"
        />
        <el-button type="primary" style="margin-left: 8px" :loading="addStudentLoading" @click="addStudent">添加学生</el-button>
      </div>
      <p class="hint">学生用户 ID 可在「用户管理」中查看；须为学生角色账号。</p>
      <el-table :data="studentList" stripe max-height="360">
        <el-table-column prop="studentId" label="学生ID" width="100" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="joinTime" label="加入时间" width="170" />
        <el-table-column label="操作" width="88">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeStudent(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
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
  teacherId: number;
  teacherName?: string;
  name: string;
  description?: string;
  code?: string;
  status: number;
  createTime?: string;
}

interface CoursePage {
  records: CourseRow[];
  total: number;
}

interface CourseStudentRow {
  id: number;
  studentId: number;
  username?: string;
  realName?: string;
  status: number;
  joinTime?: string;
}

const isAdmin = computed(() => localStorage.getItem('role') === 'ADMIN');

interface InstructorOption {
  id: number;
  label: string;
}
const instructorOptions = ref<InstructorOption[]>([]);

interface UserRow {
  id: number;
  username: string;
  realName?: string;
  role: string;
}

const query = reactive({ keyword: '' });
const list = ref<CourseRow[]>([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);

const createVisible = ref(false);
const createSubmitting = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = reactive({
  name: '',
  code: '',
  description: '',
  teacherId: undefined as number | undefined
});

const createRules: FormRules = {
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { min: 1, max: 128, message: '长度 1～128', trigger: 'blur' }
  ],
  code: [{ max: 64, message: '课程代码过长', trigger: 'blur' }],
  description: [{ max: 512, message: '简介过长', trigger: 'blur' }]
};

const editVisible = ref(false);
const editSubmitting = ref(false);
const editFormRef = ref<FormInstance>();
const editingId = ref<number | null>(null);
const editForm = reactive({
  teacherId: undefined as number | undefined,
  name: '',
  code: '',
  description: '',
  status: 1
});

const editRules: FormRules = {
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { min: 1, max: 128, message: '长度 1～128', trigger: 'blur' }
  ],
  code: [{ max: 64, message: '课程代码过长', trigger: 'blur' }],
  description: [{ max: 512, message: '简介过长', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
};

const studentsVisible = ref(false);
const studentsCourse = ref<CourseRow | null>(null);
const studentsTitle = computed(() =>
  studentsCourse.value ? `选课学生 — ${studentsCourse.value.name}` : '选课学生'
);
const studentList = ref<CourseStudentRow[]>([]);
const studentIdInput = ref('');
const addStudentLoading = ref(false);

async function loadInstructorOptions() {
  if (!isAdmin.value) return;
  const { data } = await http.get<{ records: UserRow[] }>('/api/system/user', {
    params: { page: 1, size: 200 }
  });
  const roleLabel = (r: string) => (r === 'ADMIN' ? '管理员' : '教师');
  instructorOptions.value = (data?.records ?? [])
    .filter((u) => u.role === 'TEACHER' || u.role === 'ADMIN')
    .map((u) => ({
      id: u.id,
      label: `${u.username}${u.realName ? '（' + u.realName + '）' : ''} · ${roleLabel(u.role)}`
    }));
}

const fetchData = async () => {
  const { data } = await http.get<CoursePage>('/api/course', {
    params: {
      page: page.value,
      size: size.value,
      keyword: query.keyword.trim() || undefined
    }
  });
  list.value = data?.records ?? [];
  total.value = data?.total ?? 0;
};

function openCreate() {
  resetCreate();
  createVisible.value = true;
}

function resetCreate() {
  createForm.name = '';
  createForm.code = '';
  createForm.description = '';
  createForm.teacherId = undefined;
  createFormRef.value?.resetFields();
}

const submitCreate = async () => {
  const el = createFormRef.value;
  if (!el) return;
  await el.validate(async (valid) => {
    if (!valid) return;
    createSubmitting.value = true;
    try {
      await http.post('/api/course', {
        name: createForm.name.trim(),
        code: createForm.code.trim() || undefined,
        description: createForm.description.trim() || undefined,
        teacherId: isAdmin.value && createForm.teacherId != null ? createForm.teacherId : undefined
      });
      ElMessage.success('创建成功');
      createVisible.value = false;
      await fetchData();
    } finally {
      createSubmitting.value = false;
    }
  });
};

function openEdit(row: CourseRow) {
  editingId.value = row.id;
  editForm.teacherId = row.teacherId;
  editForm.name = row.name;
  editForm.code = row.code ?? '';
  editForm.description = row.description ?? '';
  editForm.status = row.status;
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
      await http.put(`/api/course/${editingId.value}`, {
        name: editForm.name.trim(),
        code: editForm.code.trim() || undefined,
        description: editForm.description.trim() || undefined,
        status: editForm.status,
        teacherId: isAdmin.value && editForm.teacherId != null ? editForm.teacherId : undefined
      });
      ElMessage.success('保存成功');
      editVisible.value = false;
      await fetchData();
    } finally {
      editSubmitting.value = false;
    }
  });
};

async function loadStudents() {
  if (!studentsCourse.value) return;
  const { data } = await http.get<CourseStudentRow[]>(`/api/course/${studentsCourse.value.id}/students`);
  studentList.value = data ?? [];
}

function openStudents(row: CourseRow) {
  studentsCourse.value = row;
  studentIdInput.value = '';
  studentsVisible.value = true;
  loadStudents();
}

async function onDelete(row: CourseRow) {
  try {
    await ElMessageBox.confirm(`确定删除课程「${row.name}」吗？`, '删除课程', {
      type: 'warning'
    });
  } catch {
    return;
  }
  await http.delete(`/api/course/${row.id}`);
  ElMessage.success('删除成功');
  await fetchData();
}

function resetStudents() {
  studentsCourse.value = null;
  studentList.value = [];
  studentIdInput.value = '';
}

const addStudent = async () => {
  if (!studentsCourse.value) return;
  const sid = Number(studentIdInput.value.trim());
  if (!Number.isFinite(sid) || sid <= 0) {
    ElMessage.warning('请输入有效的学生用户 ID');
    return;
  }
  addStudentLoading.value = true;
  try {
    await http.post(`/api/course/${studentsCourse.value.id}/students/${sid}`);
    ElMessage.success('已添加');
    studentIdInput.value = '';
    await loadStudents();
  } finally {
    addStudentLoading.value = false;
  }
};

const removeStudent = async (row: CourseStudentRow) => {
  if (!studentsCourse.value) return;
  try {
    await ElMessageBox.confirm(`确定将「${row.username ?? row.studentId}」移出课程？`, '移除学生', {
      type: 'warning'
    });
  } catch {
    return;
  }
  await http.delete(`/api/course/${studentsCourse.value.id}/students/${row.studentId}`);
  ElMessage.success('已移除');
  await loadStudents();
};

onMounted(async () => {
  await fetchData();
  await loadInstructorOptions();
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
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.student-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.hint {
  margin: 0 0 12px;
  font-size: 12px;
  color: #909399;
}
</style>
