<template>
  <div class="course-page">
    <!-- 顶部功能区 -->
    <div class="panel panel-top">
      <h1 class="page-title">课程管理</h1>
      <el-button type="primary" size="large" class="btn-new" @click="openCreate">新建课程</el-button>
    </div>

    <!-- 中部检索区 -->
    <div class="panel panel-search">
      <div class="search-inner">
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索课程名 / 代码"
          clearable
          @keyup.enter="load"
        />
        <el-button type="primary" size="large" class="search-btn" @click="load">查询</el-button>
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
      >
        <template #empty>
          <div class="table-empty">No Data</div>
        </template>
        <el-table-column prop="id" label="ID" width="72" align="center" />
        <el-table-column prop="name" label="课程名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="code" label="代码" width="140" align="center" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="goPreview(row.id)">查看</el-button>
            <el-button type="primary" link @click="openStudents(row)">学生</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import type { CSSProperties } from 'vue';
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

const headerCellStyle = (): CSSProperties => ({
  background: '#f5f7fa',
  fontWeight: '700',
  textAlign: 'center',
  color: '#303133'
});

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
.course-page {
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
  padding: 20px 24px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.02em;
}

.btn-new {
  min-width: 120px;
  font-weight: 600;
  border-radius: 8px;
}

.panel-search {
  padding: 18px 24px;
}

.search-inner {
  display: flex;
  align-items: stretch;
  max-width: 100%;
  width: 100%;
}

.search-input {
  flex: 1;
  min-width: 0;
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
