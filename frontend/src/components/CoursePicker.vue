<template>
  <div class="course-picker">
    <span class="label">当前课程</span>
    <el-select
      :model-value="courseId ?? undefined"
      placeholder="请选择课程"
      filterable
      clearable
      style="min-width: 220px"
      @update:model-value="onChange"
    >
      <el-option v-for="c in courses" :key="c.id" :label="optionLabel(c)" :value="c.id" />
    </el-select>
    <el-button v-if="showRefresh" text type="primary" @click="load">刷新</el-button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchCoursePage } from '@/api/modules/course';
import { SAMPLE_SUBJECT_COURSES, isSampleCourseId, mergeCoursesWithSamples } from '@/data/sampleCourses';
import type { Course } from '@/types/models';

function optionLabel(c: Course) {
  return isSampleCourseId(c.id) ? `${c.name}（示例）` : c.name;
}

withDefaults(
  defineProps<{
    showRefresh?: boolean;
  }>(),
  { showRefresh: true }
);

const store = useCourseContextStore();
const { courseId } = storeToRefs(store);
const courses = ref<Course[]>([]);

async function load() {
  try {
    const { data } = await fetchCoursePage(1, 500);
    const apiRows = data?.records ?? [];
    courses.value = mergeCoursesWithSamples(apiRows);
  } catch {
    courses.value = [...SAMPLE_SUBJECT_COURSES];
  }
  if (courseId.value == null && courses.value.length) {
    store.setCourseId(courses.value[0].id);
  }
}

function onChange(v: number | undefined) {
  store.setCourseId(v ?? null);
}

onMounted(() => {
  store.loadFromStorage();
  void load();
});

defineExpose({ load });
</script>

<style scoped>
.course-picker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.label {
  font-size: 13px;
  color: #606266;
}
</style>
