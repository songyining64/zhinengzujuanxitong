<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索用户名/姓名" clearable style="width: 220px" />
        <el-select v-model="query.role" clearable placeholder="角色" style="width: 140px">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="学生" value="STUDENT" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="visible" :title="title" width="480px" destroy-on-close @closed="reset">
      <el-form :model="form" label-width="88px">
        <el-form-item v-if="!editingId" label="用户名" required>
          <el-input v-model="form.username" autocomplete="off" />
        </el-form-item>
        <el-form-item :label="editingId ? '新密码' : '密码'" :required="!editingId">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="editingId ? '不修改请留空' : ''"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchUserPage, createUser, updateUser, type UserRow } from '@/api/modules/user';

const query = reactive({
  keyword: '',
  role: '' as string | undefined
});

const list = ref<UserRow[]>([]);
const loading = ref(false);
const page = ref(1);
const size = ref(10);
const total = ref(0);

const visible = ref(false);
const title = ref('新增用户');
const editingId = ref<number | null>(null);
const saving = ref(false);
const form = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'STUDENT',
  status: 1
});

const fetchData = async () => {
  loading.value = true;
  try {
    const data = await fetchUserPage({
      keyword: query.keyword || undefined,
      role: query.role || undefined,
      page: page.value,
      size: size.value
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

function reset() {
  form.username = '';
  form.password = '';
  form.realName = '';
  form.role = 'STUDENT';
  form.status = 1;
}

function openCreate() {
  editingId.value = null;
  title.value = '新增用户';
  reset();
  visible.value = true;
}

function openEdit(row: UserRow) {
  editingId.value = row.id;
  title.value = '编辑用户';
  form.username = row.username;
  form.password = '';
  form.realName = row.realName || '';
  form.role = row.role;
  form.status = row.status;
  visible.value = true;
}

async function save() {
  if (!editingId.value) {
    if (!form.username.trim() || !form.password) {
      ElMessage.warning('请填写用户名与密码');
      return;
    }
  }
  saving.value = true;
  try {
    if (editingId.value) {
      await updateUser(editingId.value, {
        realName: form.realName || undefined,
        role: form.role,
        status: form.status,
        password: form.password || undefined
      });
      ElMessage.success('已保存');
    } else {
      await createUser({
        username: form.username.trim(),
        password: form.password,
        realName: form.realName || undefined,
        role: form.role,
        status: form.status
      });
      ElMessage.success('已创建');
    }
    visible.value = false;
    await fetchData();
  } catch {
    /* */
  } finally {
    saving.value = false;
  }
}

onMounted(fetchData);
</script>

<style scoped>
.page {
  max-width: 1100px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
