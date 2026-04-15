<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">菜单管理</span>
      </div>
    </template>

    <p class="hint">
      当前后端仅提供只读菜单树 <code>/api/system/menu/tree</code>（与侧栏同源）。此处仅供管理员查看结构，无法在页面内增删改菜单。
    </p>

    <div class="toolbar">
      <el-button type="primary" @click="fetchMenus">刷新</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="list"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="name" label="栏目名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip />
      <el-table-column prop="perms" label="权限标识" min-width="180" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="90" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import http from '@/api/http';
import type { MenuTreeVO } from '@/types/menu';

const list = ref<MenuTreeVO[]>([]);
const loading = ref(false);

async function fetchMenus() {
  loading.value = true;
  try {
    const { data } = await http.get<MenuTreeVO[]>('/api/system/menu/tree');
    list.value = data ?? [];
  } finally {
    loading.value = false;
  }
}

onMounted(fetchMenus);
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-weight: 600;
}

.toolbar {
  margin-bottom: 12px;
}

.hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.hint code {
  font-size: 12px;
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
