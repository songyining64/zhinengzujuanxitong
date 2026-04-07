<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>课程管理</span>
        <el-button type="primary" @click="openCreate">新建课程</el-button>
      </div>
    </template>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索课程名/代码" clearable style="width: 240px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="课程名称" min-width="160" />
      <el-table-column prop="code" label="代码" width="120" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="primary" link @click="openStudents(row)">学生</el-button>
          <el-button type="primary" link @click="goPreview(row.id)">预览</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />

    <el-dialog v-model="editVisible" :title="editId ? '编辑课程' : '新建课程'" width="520px" destroy-on-close @closed="resetForm">
      <el-form :model="form" label-width="88px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="课程名称" />
        </el-form-item>
        <el-form-item label="代码">
          <el-input v-model="form.code" placeholder="可选" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item v-if="editId" label="状态">
          <el-switch v-model="form.statusActive" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stuVisible" title="课程学生" width="640px" destroy-on-close>
      <div v-if="currentCourse" class="stu-bar">
        <span>{{ currentCourse.name }}</span>
        <el-select
          v-model="pickStudentId"
          filterable
          remote
          clearable
          reserve-keyword
          placeholder="搜索学生用户名/姓名"
          :remote-method="searchStudents"
          :loading="stuSearchLoading"
          style="width: 260px"
        >
          <el-option v-for="u in stuOptions" :key="u.id" :label="`${u.username} ${u.realName || ''}`" :value="u.id" />
        </el-select>
        <el-button type="primary" :disabled="!pickStudentId" @click="addStudent">加入</el-button>
      </div>
      <el-table :data="stuList" size="small" class="stu-table">
        <el-table-column prop="studentId" label="学生ID" width="88" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" link @click="removeStudent(row.studentId)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  fetchCoursePage,
  createCourse,
  updateCourse,
  fetchCourseStudents,
  addCourseStudent,
  removeCourseStudent,
  fetchStudentCandidates,
  type CourseRow
} from '@/api/course';

const router = useRouter();
const loading = ref(false);
const rows = ref<CourseRow[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');

const editVisible = ref(false);
const editId = ref<number | null>(null);
const saving = ref(false);
const form = reactive({
  name: '',
  code: '',
  description: '',
  statusActive: true
});

const stuVisible = ref(false);
const currentCourse = ref<CourseRow | null>(null);
const stuList = ref<{ studentId: number; username?: string; realName?: string }[]>([]);
const pickStudentId = ref<number | null>(null);
const stuOptions = ref<{ id: number; username: string; realName?: string }[]>([]);
const stuSearchLoading = ref(false);

async function load() {
  loading.value = true;
  try {
    const { data } = await fetchCoursePage({ page: page.value, size: size.value, keyword: keyword.value || undefined });
    rows.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editId.value = null;
  resetForm();
  editVisible.value = true;
}

function openEdit(row: CourseRow) {
  editId.value = row.id;
  form.name = row.name;
  form.code = row.code || '';
  form.description = row.description || '';
  form.statusActive = row.status !== 0;
  editVisible.value = true;
}

function resetForm() {
  form.name = '';
  form.code = '';
  form.description = '';
  form.statusActive = true;
}

async function saveCourse() {
  if (!form.name.trim()) return;
  saving.value = true;
  try {
    if (editId.value) {
      await updateCourse(editId.value, {
        name: form.name,
        code: form.code || undefined,
        description: form.description || undefined,
        status: form.statusActive ? 1 : 0
      });
    } else {
      await createCourse({
        name: form.name,
        code: form.code || undefined,
        description: form.description || undefined
      });
    }
    editVisible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

function goPreview(id: number) {
  router.push({ name: 'CoursePreview', params: { id: String(id) } });
}

async function openStudents(row: CourseRow) {
  currentCourse.value = row;
  pickStudentId.value = null;
  stuOptions.value = [];
  stuVisible.value = true;
  const { data } = await fetchCourseStudents(row.id);
  stuList.value = data ?? [];
}

async function searchStudents(q: string) {
  if (!q) {
    stuOptions.value = [];
    return;
  }
  stuSearchLoading.value = true;
  try {
    const { data } = await fetchStudentCandidates({ page: 1, size: 20, keyword: q });
    stuOptions.value = data?.records ?? [];
  } finally {
    stuSearchLoading.value = false;
  }
}

async function addStudent() {
  if (!currentCourse.value || !pickStudentId.value) return;
  await addCourseStudent(currentCourse.value.id, pickStudentId.value);
  pickStudentId.value = null;
  const { data } = await fetchCourseStudents(currentCourse.value.id);
  stuList.value = data ?? [];
}

async function removeStudent(sid: number) {
  if (!currentCourse.value) return;
  await removeCourseStudent(currentCourse.value.id, sid);
  const { data } = await fetchCourseStudents(currentCourse.value.id);
  stuList.value = data ?? [];
}

onMounted(load);
</script>

<style scoped>
.head,
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.toolbar {
  margin-bottom: 12px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.stu-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.stu-table {
  width: 100%;
}
</style>
