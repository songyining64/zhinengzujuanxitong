<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <span>自动组卷</span>
        <CoursePicker />
      </div>
    </template>

    <el-alert v-if="!courseId" type="info" show-icon :closable="false" title="请先选择课程" />

    <el-row v-else :gutter="20" class="compose-row">
      <el-col :xs="24" :lg="14">
        <el-form :model="form" label-width="140px" class="form" @submit.prevent>
      <el-form-item label="试卷标题" required>
        <el-input v-model="form.title" placeholder="如：第3章单元测验" style="max-width: 420px" />
      </el-form-item>
      <el-form-item label="全课程随机池">
        <el-switch v-model="form.randomPool" />
        <span class="hint">开启后忽略下方知识点范围</span>
      </el-form-item>
      <el-form-item v-if="!form.randomPool" label="知识点">
        <el-select
          v-model="form.knowledgePointIds"
          multiple
          filterable
          collapse-tags
          placeholder="选择知识点"
          style="width: 100%; max-width: 520px"
        >
          <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!form.randomPool" label="包含子孙知识点">
        <el-switch v-model="form.includeKnowledgeDescendants" />
      </el-form-item>
      <el-form-item label="必考题 ID">
        <el-input
          v-model="fixedIdsText"
          placeholder="逗号分隔，如：101,102"
          style="max-width: 420px"
        />
      </el-form-item>
      <el-form-item label="题型数量">
        <div class="grid">
          <div v-for="t in typeFields" :key="t.key" class="cell">
            <span>{{ t.label }}</span>
            <el-input-number v-model="counts[t.key]" :min="0" :max="999" controls-position="right" />
          </div>
        </div>
      </el-form-item>
      <el-form-item label="每题分值">
        <el-input-number v-model="form.scorePerQuestion" :min="1" :step="1" />
      </el-form-item>
      <el-form-item label="难度权重">
        <div class="grid small">
          <div class="cell"><span>易 EASY</span><el-input-number v-model="weights.EASY" :min="0" /></div>
          <div class="cell"><span>中 MEDIUM</span><el-input-number v-model="weights.MEDIUM" :min="0" /></div>
          <div class="cell"><span>难 HARD</span><el-input-number v-model="weights.HARD" :min="0" /></div>
        </div>
        <p class="hint">全 0 表示不按难度分层，仅在题型内随机</p>
      </el-form-item>
      <el-form-item label="随机种子">
        <el-input-number v-model="form.randomSeed" :step="1" controls-position="right" />
        <span class="hint">可空，便于复现同一套卷</span>
      </el-form-item>
      <el-form-item label="跨题型去重">
        <el-switch v-model="form.dedup" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">生成试卷</el-button>
        <el-button @click="$router.push('/paper')">返回试卷库</el-button>
      </el-form-item>
    </el-form>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="preview-card">
          <template #header>
            <span>实时预览</span>
            <el-tag size="small" type="success">参数变更即更新</el-tag>
          </template>
          <h3 class="pv-title">{{ form.title?.trim() || '（未命名试卷）' }}</h3>
          <el-descriptions :column="1" size="small" border class="pv-meta">
            <el-descriptions-item label="选题范围">
              {{ form.randomPool ? '全课程随机池' : `已选 ${form.knowledgePointIds.length} 个知识点` }}
            </el-descriptions-item>
            <el-descriptions-item label="必考题">{{ fixedPreview }}</el-descriptions-item>
            <el-descriptions-item label="去重">{{ form.dedup ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="随机种子">{{ form.randomSeed ?? '（系统随机）' }}</el-descriptions-item>
          </el-descriptions>
          <el-divider content-position="left">大题结构</el-divider>
          <ol class="pv-list">
            <li v-for="line in previewLines" :key="line.key">
              <strong>{{ line.label }}</strong> {{ line.n }} 题 × {{ perScore }} 分 = {{ line.subtotal }} 分
            </li>
            <li v-if="!previewLines.length" class="muted">请设置题型数量</li>
          </ol>
          <div class="pv-total">
            预计客观/主观题量合计：<b>{{ previewQuestionCount }}</b> 题，
            总分约 <b>{{ previewTotalScore }}</b> 分
          </div>
          <p class="pv-hint">正式题目与难度配比以后端抽题为准，此预览仅反映题量与分值配置。</p>
        </el-card>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import CoursePicker from '@/components/CoursePicker.vue';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchKnowledgeList } from '@/api/modules/knowledge';
import { createAutoPaper } from '@/api/modules/paper';
import type { PaperAutoGenRequest } from '@/types/models';

const router = useRouter();
const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

const knowledgePoints = ref<{ id: number; name: string }[]>([]);

const typeFields = [
  { key: 'SINGLE', label: '单选' },
  { key: 'MULTIPLE', label: '多选' },
  { key: 'TRUE_FALSE', label: '判断' },
  { key: 'FILL', label: '填空' },
  { key: 'SHORT', label: '简答' }
];

const counts = reactive<Record<string, number>>({
  SINGLE: 5,
  MULTIPLE: 3,
  TRUE_FALSE: 2,
  FILL: 0,
  SHORT: 1
});

const weights = reactive({ EASY: 0, MEDIUM: 0, HARD: 0 });

const form = reactive({
  title: '',
  randomPool: false,
  knowledgePointIds: [] as number[],
  includeKnowledgeDescendants: true,
  scorePerQuestion: 10 as number | undefined,
  randomSeed: undefined as number | undefined,
  dedup: true
});

const fixedIdsText = ref('');

watch(courseId, async () => {
  form.knowledgePointIds = [];
  if (!courseId.value) {
    knowledgePoints.value = [];
    return;
  }
  const { data } = await fetchKnowledgeList(courseId.value);
  knowledgePoints.value = (data ?? []).map((k) => ({ id: k.id, name: k.name }));
}, { immediate: true });

const submitting = ref(false);

const perScore = computed(() => form.scorePerQuestion ?? 10);

const previewLines = computed(() =>
  typeFields
    .map((t) => {
      const n = counts[t.key] ?? 0;
      if (n <= 0) return null;
      return {
        key: t.key,
        label: t.label,
        n,
        subtotal: n * perScore.value
      };
    })
    .filter(Boolean) as { key: string; label: string; n: number; subtotal: number }[]
);

const previewQuestionCount = computed(() => previewLines.value.reduce((s, l) => s + l.n, 0));

const previewTotalScore = computed(() => previewLines.value.reduce((s, l) => s + l.subtotal, 0));

const fixedPreview = computed(() => {
  const ids = fixedIdsText.value
    .split(/[,，\s]+/)
    .map((s) => s.trim())
    .filter(Boolean);
  return ids.length ? ids.join(', ') : '无';
});

function buildBody(): PaperAutoGenRequest {
  const countByType: Record<string, number> = {};
  for (const t of typeFields) {
    const n = counts[t.key] ?? 0;
    if (n > 0) countByType[t.key] = n;
  }
  const wSum = weights.EASY + weights.MEDIUM + weights.HARD;
  const difficultyWeights =
    wSum > 0
      ? { EASY: weights.EASY || 0, MEDIUM: weights.MEDIUM || 0, HARD: weights.HARD || 0 }
      : undefined;

  const fixedQuestionIds = fixedIdsText.value
    .split(/[,，\s]+/)
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isFinite(n) && n > 0);

  return {
    courseId: courseId.value!,
    title: form.title.trim(),
    randomPool: form.randomPool,
    knowledgePointIds: form.randomPool ? undefined : form.knowledgePointIds,
    includeKnowledgeDescendants: form.includeKnowledgeDescendants,
    fixedQuestionIds: fixedQuestionIds.length ? fixedQuestionIds : undefined,
    scorePerQuestion: form.scorePerQuestion,
    randomSeed: form.randomSeed ?? undefined,
    dedup: form.dedup,
    countByType: Object.keys(countByType).length ? countByType : undefined,
    difficultyWeights
  };
}

async function submit() {
  if (!courseId.value || !form.title.trim()) {
    ElMessage.warning('请填写试卷标题');
    return;
  }
  const body = buildBody();
  if (!body.randomPool && (!body.knowledgePointIds || body.knowledgePointIds.length === 0)) {
    ElMessage.warning('请选择知识点或开启全课程随机池');
    return;
  }
  if (!body.countByType || Object.keys(body.countByType).length === 0) {
    ElMessage.warning('请至少为一种题型设置题量');
    return;
  }
  submitting.value = true;
  try {
    const { data } = await createAutoPaper(body);
    ElMessage.success('组卷成功');
    await router.push(`/paper/${data.id}`);
  } finally {
    submitting.value = false;
  }
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

.form {
  max-width: 720px;
}

.hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 24px;
}

.grid.small .cell {
  align-items: center;
}

.cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.compose-row {
  align-items: flex-start;
}

.preview-card :deep(.el-card__header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pv-title {
  margin: 0 0 12px;
  font-size: 18px;
}

.pv-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}

.pv-total {
  margin-top: 12px;
  padding: 10px;
  background: #ecf5ff;
  border-radius: 6px;
  font-size: 14px;
}

.pv-hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.muted {
  color: #c0c4cc;
}
</style>
