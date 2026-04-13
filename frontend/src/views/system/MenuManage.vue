<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">菜单管理</span>
      </div>
    </template>

    <div class="toolbar">
      <el-button type="primary" @click="fetchMenus">刷新</el-button>
    </div>

    <el-table :data="list" row-key="id" border default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="name" label="栏目名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip />
      <el-table-column prop="perms" label="权限标识" min-width="180" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '@/api/http';

interface MenuRow {
  id: number;
  parentId?: number | null;
  name: string;
  path?: string | null;
  perms?: string | null;
  sortOrder?: number | null;
  children?: MenuRow[];
}

const list = ref<MenuRow[]>([]);

function buildTree(items: MenuRow[]): MenuRow[] {
  const map = new Map<number, MenuRow>();
  items.forEach((i) => map.set(i.id, { ...i, children: [] }));
  const roots: MenuRow[] = [];
  for (const i of items) {
    const node = map.get(i.id)!;
    const pid = i.parentId ?? null;
    if (pid == null) {
      roots.push(node);
      continue;
    }
    const parent = map.get(pid);
    if (parent) {
      parent.children!.push(node);
    } else {
      roots.push(node);
    }
  }
  const sortRec = (nodes: MenuRow[]) => {
    nodes.sort((a, b) => ((a.sortOrder ?? 0) - (b.sortOrder ?? 0)) || (a.id - b.id));
    nodes.forEach((n) => {
      if (n.children?.length) sortRec(n.children);
      else delete n.children;
    });
  };
  sortRec(roots);
  return roots;
}

async function fetchMenus() {
  const { data } = await http.get<MenuRow[]>('/api/system/menu/all');
  list.value = buildTree(data ?? []);
}

async function confirmDelete(row: MenuRow) {
  await ElMessageBox.confirm(
    `确定删除栏目「${row.name}」吗？其子栏目也会一并删除。`,
    '删除确认',
    { type: 'warning' }
  );
  await http.delete(`/api/system/menu/${row.id}`);
  ElMessage.success('删除成功');
  await fetchMenus();
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
  font-size: 16px;
  font-weight: 600;
}

.toolbar {
  margin-bottom: 12px;
}
</style>
