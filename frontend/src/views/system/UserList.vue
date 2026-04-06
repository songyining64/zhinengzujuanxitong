<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索用户名/姓名" style="width: 220px" clearable @keyup.enter="fetchData" />
      <el-button type="primary" style="margin-left: 8px" @click="fetchData">搜索</el-button>
      <el-button type="primary" style="margin-left: auto" @click="openCreate">新增用户</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="{ row }">
          {{ roleLabel(row.role) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增用户" width="480px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录名，唯一" maxlength="64" show-word-limit autocomplete="off" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="至少 6 位"
            maxlength="64"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="可选" maxlength="64" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择" style="width: 100%">
            <el-option v-for="o in roleOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '@/api/http';

const query = reactive({
  keyword: ''
});

interface UserRow {
  id: number;
  username: string;
  realName?: string;
  role: string;
  status: number;
}

const list = ref<UserRow[]>([]);

const dialogVisible = ref(false);
const submitting = ref(false);
const formRef = ref<FormInstance>();

const roleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '教师', value: 'TEACHER' },
  { label: '学生', value: 'STUDENT' }
];

const form = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'TEACHER' as string,
  status: 1
});

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 64, message: '长度为 2～64', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为 6～64', trigger: 'blur' }
  ],
  realName: [{ max: 64, message: '姓名过长', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
};

function roleLabel(role: string) {
  const m: Record<string, string> = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' };
  return m[role] ?? role;
}

const fetchData = async () => {
  const { data } = await http.get<{ records: UserRow[] }>('/api/system/user', { params: query });
  list.value = data?.records ?? [];
};

function openCreate() {
  resetForm();
  dialogVisible.value = true;
}

function resetForm() {
  form.username = '';
  form.password = '';
  form.realName = '';
  form.role = 'TEACHER';
  form.status = 1;
  formRef.value?.resetFields();
}

const submitCreate = async () => {
  const el = formRef.value;
  if (!el) return;
  await el.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      await http.post('/api/system/user', {
        username: form.username.trim(),
        password: form.password,
        realName: form.realName.trim() || undefined,
        role: form.role,
        status: form.status
      });
      ElMessage.success('创建成功');
      dialogVisible.value = false;
      await fetchData();
    } finally {
      submitting.value = false;
    }
  });
};

const confirmDelete = async (row: UserRow) => {
  await ElMessageBox.confirm(
    `确定删除用户「${row.username}」吗？`,
    '删除用户',
    { type: 'warning' }
  );
  await http.delete(`/api/system/user/${row.id}`);
  ElMessage.success('删除成功');
  await fetchData();
};

onMounted(fetchData);
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
</style>
