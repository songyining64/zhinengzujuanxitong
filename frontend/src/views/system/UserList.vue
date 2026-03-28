<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索用户名/姓名" clearable style="width: 220px" @keyup.enter="fetchData" />
      <el-button type="primary" @click="fetchData" style="margin-left: 8px">搜索</el-button>
      <el-button type="primary" @click="onCreate" style="margin-left: auto">新增用户</el-button>
    </div>

    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="role" label="角色" />
      <el-table-column prop="status" label="状态" width="80" />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pager"
      @current-change="fetchData"
      @size-change="fetchData"
    />

    <el-dialog v-model="createVisible" title="新增用户" width="440px" destroy-on-close>
      <el-form :model="createForm" label-width="88px">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" autocomplete="off" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="createForm.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="createForm.realName" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option label="管理员 ADMIN" value="ADMIN" />
            <el-option label="教师 TEACHER" value="TEACHER" />
            <el-option label="学生 STUDENT" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="createForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import http from '@/api/http';

const query = reactive({
  keyword: ''
});

const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);

interface UserRow {
  id: number;
  username: string;
  realName?: string;
  role: string;
  status: number;
}

const list = ref<UserRow[]>([]);

const createVisible = ref(false);
const creating = ref(false);
const createForm = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'STUDENT',
  status: 1
});

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await http.get<{ records: UserRow[]; total: number }>('/api/system/user', {
      params: { keyword: query.keyword || undefined, page: page.value, size: size.value }
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

const onCreate = () => {
  createForm.username = '';
  createForm.password = '';
  createForm.realName = '';
  createForm.role = 'STUDENT';
  createForm.status = 1;
  createVisible.value = true;
};

const submitCreate = async () => {
  if (!createForm.username.trim() || createForm.password.length < 6) return;
  creating.value = true;
  try {
    await http.post('/api/system/user', {
      username: createForm.username.trim(),
      password: createForm.password,
      realName: createForm.realName?.trim() || undefined,
      role: createForm.role,
      status: createForm.status
    });
    createVisible.value = false;
    await fetchData();
  } finally {
    creating.value = false;
  }
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
</style>
