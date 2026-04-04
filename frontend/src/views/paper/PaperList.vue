<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <span>试卷库</span>
        <div class="actions">
          <CoursePicker />
          <el-button type="primary" @click="$router.push('/paper/compose/auto')">自动组卷</el-button>
          <el-button @click="$router.push('/paper/compose/manual')">手工组卷</el-button>
        </div>
      </div>
    </template>

    <el-alert v-if="!courseId" type="info" show-icon :closable="false" title="请先选择课程" />

    <template v-else>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="mode" label="模式" width="100" />
        <el-table-column prop="totalScore" label="总分" width="88" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { ElMessage, ElMessageBox } from 'element-plus';
import CoursePicker from '@/components/CoursePicker.vue';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchPapers, deletePaper } from '@/api/modules/paper';
import type { Paper } from '@/types/models';

const router = useRouter();
const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

function goDetail(id: number) {
  void router.push(`/paper/${id}`);
}

const loading = ref(false);
const list = ref<Paper[]>([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchPapers(courseId.value, page.value, size.value);
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

watch(courseId, () => {
  page.value = 1;
  list.value = [];
  total.value = 0;
  if (courseId.value) void load();
}, { immediate: true });

async function onDelete(row: Paper) {
  try {
    await ElMessageBox.confirm(`确定删除试卷「${row.title}」？`, '确认');
  } catch {
    return;
  }
  await deletePaper(row.id);
  ElMessage.success('已删除');
  await load();
}
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
