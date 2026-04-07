<template>
  <el-card v-loading="loading" shadow="never">
    <template #header>
      <div class="head">
        <el-button link @click="$router.push('/paper')">← 返回列表</el-button>
        <span v-if="detail">试卷：{{ detail.paper.title }}</span>
      </div>
    </template>

    <template v-if="detail">
      <el-descriptions :column="3" border class="meta">
        <el-descriptions-item label="ID">{{ detail.paper.id }}</el-descriptions-item>
        <el-descriptions-item label="课程ID">{{ detail.paper.courseId }}</el-descriptions-item>
        <el-descriptions-item label="模式">{{ detail.paper.mode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ detail.paper.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="随机种子">{{ detail.paper.randomSeed ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.paper.createTime || '—' }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="sub">题目明细</h3>
      <el-table :data="detail.questions" stripe>
        <el-table-column prop="questionOrder" label="#" width="56" />
        <el-table-column prop="type" label="题型" width="96" />
        <el-table-column prop="score" label="分值" width="72" />
        <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column prop="questionId" label="题目ID" width="88" />
      </el-table>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { getPaperDetail } from '@/api/modules/paper';
import type { PaperDetailVO } from '@/types/models';

const route = useRoute();
const loading = ref(false);
const detail = ref<PaperDetailVO | null>(null);

async function load() {
  const id = Number(route.params.id);
  if (!Number.isFinite(id)) return;
  loading.value = true;
  try {
    const { data } = await getPaperDetail(id);
    detail.value = data ?? null;
  } finally {
    loading.value = false;
  }
}

watch(() => route.params.id, load, { immediate: true });
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta {
  margin-bottom: 16px;
}

.sub {
  margin: 16px 0 12px;
  font-size: 16px;
  font-weight: 600;
}
</style>
