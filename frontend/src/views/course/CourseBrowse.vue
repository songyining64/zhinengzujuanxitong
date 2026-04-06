<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索课程名称/课程代码"
        style="width: 240px"
        clearable
        @keyup.enter="fetchData"
      />
      <el-button type="primary" style="margin-left: 8px" @click="fetchData">搜索</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="课程名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="code" label="课程代码" width="120" show-overflow-tooltip />
      <el-table-column prop="description" label="简介" min-width="160" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="96">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="88" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="课程详情" width="520px" destroy-on-close>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="课程代码">{{ detail.code || '—' }}</el-descriptions-item>
        <el-descriptions-item label="简介">{{ detail.description || '—' }}</el-descriptions-item>
        <el-descriptions-item label="授课教师名称">{{ detail.teacherName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
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

const query = reactive({ keyword: '' });
const list = ref<CourseRow[]>([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);

const detailVisible = ref(false);
const detail = ref<CourseRow | null>(null);

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

async function openDetail(row: CourseRow) {
  const { data } = await http.get<CourseRow>(`/api/course/${row.id}`);
  detail.value = data ?? null;
  detailVisible.value = true;
}

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
  display: flex;
  justify-content: flex-end;
}
</style>
