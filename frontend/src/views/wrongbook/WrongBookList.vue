<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <span>错题本</span>
        <CoursePicker />
      </div>
    </template>

    <el-alert v-if="!courseId" type="info" show-icon :closable="false" title="请先选择课程" />

    <template v-else>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="questionId" label="题目ID" width="88" />
        <el-table-column prop="type" label="题型" width="88" />
        <el-table-column prop="wrongCount" label="错次" width="72" />
        <el-table-column prop="lastWrongAt" label="最近错误" width="160" />
        <el-table-column prop="stem" label="题干" min-width="240" show-overflow-tooltip />
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
import { storeToRefs } from 'pinia';
import CoursePicker from '@/components/CoursePicker.vue';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchWrongBookPage } from '@/api/modules/wrongbook';
import type { WrongBookRow } from '@/types/models';

const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

const loading = ref(false);
const list = ref<WrongBookRow[]>([]);
const page = ref(1);
const size = ref(15);
const total = ref(0);

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchWrongBookPage(courseId.value, page.value, size.value);
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
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
