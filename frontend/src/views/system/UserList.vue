<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索用户名/姓名" style="width: 220px" />
      <el-button type="primary" @click="fetchData" style="margin-left: 8px">搜索</el-button>
      <el-button type="primary" @click="onCreate" style="margin-left: auto">新增用户</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="role" label="角色" />
      <el-table-column prop="status" label="状态" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
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

const fetchData = async () => {
  const { data } = await http.get<{ records: UserRow[] }>('/api/system/user', { params: query });
  list.value = data?.records ?? [];
};

const onCreate = () => {
  // TODO: 打开弹窗新增用户
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

