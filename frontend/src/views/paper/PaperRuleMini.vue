<template>
  <div class="rule-mini">
    <el-form label-width="120px" size="small">
      <el-form-item label="全课程随机池">
        <el-switch :model-value="model.randomPool" @update:model-value="patch({ randomPool: $event as boolean })" />
      </el-form-item>
      <el-form-item v-if="!model.randomPool" label="知识点 IDs">
        <el-select
          :model-value="model.knowledgePointIds"
          multiple
          filterable
          collapse-tags
          style="width: 100%"
          @update:model-value="patch({ knowledgePointIds: $event as number[] })"
        >
          <el-option v-for="kp in knowledgePoints" :key="kp.id" :label="kp.name" :value="kp.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="包含子孙">
        <el-switch
          :model-value="model.includeKnowledgeDescendants !== false"
          @update:model-value="patch({ includeKnowledgeDescendants: $event as boolean })"
        />
      </el-form-item>
      <el-form-item label="题型数量">
        <div class="counts">
          <span v-for="t in types" :key="t.k" class="c">
            {{ t.l }}
            <el-input-number
              :model-value="countModel[t.k]"
              :min="0"
              size="small"
              controls-position="right"
              @update:model-value="onCount(t.k, $event as number | undefined)"
            />
          </span>
        </div>
      </el-form-item>
      <el-form-item label="每题分值">
        <el-input-number
          :model-value="model.scorePerQuestion"
          :min="1"
          size="small"
          @update:model-value="patch({ scorePerQuestion: $event as number })"
        />
      </el-form-item>
      <el-form-item label="去重">
        <el-switch :model-value="model.dedup !== false" @update:model-value="patch({ dedup: $event as boolean })" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchKnowledgeList } from '@/api/modules/knowledge';
import type { PaperAutoGenRequest } from '@/types/models';

const props = defineProps<{
  modelValue: PaperAutoGenRequest;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: PaperAutoGenRequest): void;
}>();

const model = computed(() => props.modelValue);

function patch(p: Partial<PaperAutoGenRequest>) {
  emit('update:modelValue', { ...props.modelValue, ...p });
}

const types = [
  { k: 'SINGLE', l: '单选' },
  { k: 'MULTIPLE', l: '多选' },
  { k: 'TRUE_FALSE', l: '判断' },
  { k: 'FILL', l: '填空' },
  { k: 'SHORT', l: '简答' }
];

const countModel = computed<Record<string, number>>(() => {
  const m: Record<string, number> = {};
  for (const t of types) m[t.k] = props.modelValue.countByType?.[t.k] ?? 0;
  return m;
});

function onCount(key: string, v: number | undefined) {
  const next = { ...(props.modelValue.countByType || {}) };
  const n = v ?? 0;
  if (n <= 0) delete next[key];
  else next[key] = n;
  patch({ countByType: Object.keys(next).length ? next : undefined });
}

const store = useCourseContextStore();
const { courseId } = storeToRefs(store);
const knowledgePoints = ref<{ id: number; name: string }[]>([]);

async function loadKp() {
  if (!courseId.value) {
    knowledgePoints.value = [];
    return;
  }
  const { data } = await fetchKnowledgeList(courseId.value);
  knowledgePoints.value = (data ?? []).map((k) => ({ id: k.id, name: k.name }));
}

onMounted(loadKp);
watch(courseId, loadKp);
</script>

<style scoped>
.rule-mini {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}

.counts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.counts .c {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 8px;
}
</style>
