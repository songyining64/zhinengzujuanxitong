<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>课程</span>
          <el-button v-if="canManage" type="primary" @click="openCreate">新建课程</el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索课程名称或编号"
          clearable
          style="max-width: 280px"
          @keyup.enter="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="name" label="课程名称" min-width="160" />
        <el-table-column prop="code" label="编号" width="120" />
        <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openStudents(row)">学员</el-button>
            <el-button v-if="canManage" link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="dlgVisible" :title="dlgTitle" width="520px" destroy-on-close @closed="resetForm">
      <el-form :model="form" label-width="96px">
        <el-form-item label="课程名称" required>
          <el-input v-model="form.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="课程编号">
          <el-input v-model="form.code" maxlength="64" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" rows="3" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item v-if="isAdmin && !editingId" label="授课教师" required>
          <el-select v-model="form.teacherId" placeholder="选择教师账号" filterable style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="`${t.realName || t.username} (${t.username})`" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stuVisible" title="课程学员" width="640px" destroy-on-close>
      <div v-if="currentCourse" class="stu-toolbar">
        <el-select
          v-model="pickStudentId"
          placeholder="选择学生加入课程"
          filterable
          style="width: 280px"
        >
          <el-option
            v-for="s in students"
            :key="s.id"
            :label="`${s.realName || s.username} (${s.username})`"
            :value="s.id"
          />
        </el-select>
        <el-button type="primary" :disabled="!pickStudentId" @click="addStudent">添加</el-button>
      </div>
      <el-table :data="courseStudents" size="small" max-height="360">
        <el-table-column prop="studentId" label="学号/用户ID" width="120" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeStudent(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import * as courseApi from '@/api/modules/course';
import type { Course } from '@/api/modules/course';
import { fetchUserPage } from '@/api/modules/user';
import { hasPerm, isRole } from '@/composables/usePermission';

const loading = ref(false);
const rows = ref<Course[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');

const canManage = computed(() => hasPerm('course:manage'));
const isAdmin = computed(() => isRole('ADMIN'));

const dlgVisible = ref(false);
const dlgTitle = ref('新建课程');
const editingId = ref<number | null>(null);
const saving = ref(false);
const form = reactive({
  name: '',
  code: '',
  description: '',
  teacherId: undefined as number | undefined
});

const teachers = ref<{ id: number; username: string; realName?: string }[]>([]);
const students = ref<{ id: number; username: string; realName?: string }[]>([]);

const stuVisible = ref(false);
const currentCourse = ref<Course | null>(null);
const courseStudents = ref<courseApi.CourseStudentRow[]>([]);
const pickStudentId = ref<number | undefined>();

async function loadTeachers() {
  const data = await fetchUserPage({ role: 'TEACHER', size: 200 });
  teachers.value = data?.records ?? [];
}

async function loadStudents() {
  const data = await fetchUserPage({ role: 'STUDENT', size: 500 });
  students.value = data?.records ?? [];
}

async function load() {
  loading.value = true;
  try {
    const data = await courseApi.fetchCoursePage({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    });
    rows.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = null;
  dlgTitle.value = '新建课程';
  resetForm();
  dlgVisible.value = true;
  if (isAdmin.value) loadTeachers();
}

function openEdit(row: Course) {
  editingId.value = row.id;
  dlgTitle.value = '编辑课程';
  form.name = row.name;
  form.code = row.code || '';
  form.description = row.description || '';
  dlgVisible.value = true;
}

function resetForm() {
  form.name = '';
  form.code = '';
  form.description = '';
  form.teacherId = undefined;
}

async function saveCourse() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写课程名称');
    return;
  }
  if (isAdmin.value && !editingId.value && !form.teacherId) {
    ElMessage.warning('请选择授课教师');
    return;
  }
  saving.value = true;
  try {
    if (editingId.value) {
      await courseApi.updateCourse(editingId.value, {
        name: form.name,
        code: form.code || undefined,
        description: form.description || undefined
      });
      ElMessage.success('已保存');
    } else {
      await courseApi.createCourse({
        name: form.name,
        code: form.code || undefined,
        description: form.description || undefined,
        teacherId: isAdmin.value ? form.teacherId : undefined
      });
      ElMessage.success('创建成功');
    }
    dlgVisible.value = false;
    await load();
  } catch {
    /* http 已提示 */
  } finally {
    saving.value = false;
  }
}

async function openStudents(row: Course) {
  currentCourse.value = row;
  pickStudentId.value = undefined;
  stuVisible.value = true;
  await loadStudents();
  const list = await courseApi.listCourseStudents(row.id);
  courseStudents.value = list ?? [];
}

async function addStudent() {
  if (!currentCourse.value || !pickStudentId.value) return;
  await courseApi.addCourseStudent(currentCourse.value.id, pickStudentId.value);
  ElMessage.success('已添加');
  pickStudentId.value = undefined;
  courseStudents.value = (await courseApi.listCourseStudents(currentCourse.value.id)) ?? [];
}

async function removeStudent(row: courseApi.CourseStudentRow) {
  if (!currentCourse.value) return;
  await courseApi.removeCourseStudent(currentCourse.value.id, row.studentId);
  ElMessage.success('已移除');
  courseStudents.value = (await courseApi.listCourseStudents(currentCourse.value.id)) ?? [];
}

onMounted(() => {
  load();
});
</script>

<style scoped>
.page {
  max-width: 1200px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.stu-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
