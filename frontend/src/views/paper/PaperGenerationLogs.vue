<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <span>组卷审计</span>
        <CoursePicker />
      </div>
    </template>

    <el-alert v-if="!courseId" type="info" show-icon :closable="false" title="请先选择课程" />

    <template v-else>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="paperId" label="试卷ID" width="88" />
        <el-table-column prop="mode" label="模式" width="100" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="createTime" label="时间" min-width="160" />
        <el-table-column label="规则摘要" min-width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="showJson(row.rulesJson)">查看 JSON</el-button>
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

    <el-dialog v-model="jsonDlg.visible" title="rulesJson" width="640px">
      <pre class="json-block">{{ jsonDlg.text }}</pre>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import CoursePicker from '@/components/CoursePicker.vue';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchPaperGenerationLogs } from '@/api/modules/paper';
import type { PaperGenerationLog } from '@/types/models';

const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

const loading = ref(false);
const list = ref<PaperGenerationLog[]>([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);

const jsonDlg = reactive({ visible: false, text: '' });

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchPaperGenerationLogs({
      courseId: courseId.value,
      page: page.value,
      size: size.value
    });
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

function showJson(raw: string | undefined) {
  if (!raw) {
    jsonDlg.text = '';
  } else {
    try {
      jsonDlg.text = JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      jsonDlg.text = raw;
    }
  }
  jsonDlg.visible = true;
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

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.json-block {
  max-height: 420px;
  overflow: auto;
  font-size: 12px;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
}
</style>
