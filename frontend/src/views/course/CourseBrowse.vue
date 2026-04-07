<template>
  <el-card>
    <template #header>
      <span>课程浏览</span>
    </template>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索课程名/代码" clearable style="width: 240px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe @row-click="onRowClick">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="课程名称" min-width="160" />
      <el-table-column prop="code" label="代码" width="120" />
      <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click.stop="openPreview(row)">预览</el-button>
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

    <el-drawer v-model="previewVisible" title="课程详情" size="420px" destroy-on-close>
      <template v-if="preview">
        <p><strong>名称：</strong>{{ preview.name }}</p>
        <p><strong>代码：</strong>{{ preview.code || '—' }}</p>
        <p><strong>简介：</strong></p>
        <p class="desc">{{ preview.description || '暂无' }}</p>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchCoursePage, getCourse, type CourseRow } from '@/api/course';

const router = useRouter();
const loading = ref(false);
const rows = ref<CourseRow[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');

const previewVisible = ref(false);
const preview = ref<CourseRow | null>(null);

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

function onRowClick(row: CourseRow) {
  router.push({ name: 'CoursePreview', params: { id: String(row.id) } });
}

async function openPreview(row: CourseRow) {
  previewVisible.value = true;
  preview.value = null;
  const { data } = await getCourse(row.id);
  preview.value = data;
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.desc {
  white-space: pre-wrap;
  color: #606266;
  line-height: 1.6;
}
</style>
