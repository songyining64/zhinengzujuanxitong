<template>
  <el-card>
    <template #header>知识点浏览</template>
    <div class="bar">
      <span>课程</span>
      <el-select v-model="courseId" placeholder="选择课程" filterable style="width: 280px" @change="load">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </div>
    <el-tree v-if="courseId" v-loading="loading" :data="tree" :props="{ label: 'label', children: 'children' }" default-expand-all />
    <el-empty v-else description="请选择课程" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import { fetchKnowledgeList, type KpRow } from '@/api/knowledge';
import { getLastCourseId, setLastCourseId } from '@/composables/useLastCourseId';

const loading = ref(false);
const courses = ref<CourseRow[]>([]);
const courseId = ref<number | null>(getLastCourseId());
const tree = ref<{ label: string; children?: unknown[] }[]>([]);

function buildTree(rows: KpRow[]) {
  type N = { id: number; label: string; children: N[] };
  const map = new Map<number, N>();
  rows.forEach((r) => {
    map.set(r.id, { id: r.id, label: r.name, children: [] });
  });
  const roots: N[] = [];
  rows.forEach((r) => {
    const n = map.get(r.id)!;
    if (r.parentId == null || !map.has(r.parentId)) {
      roots.push(n);
    } else {
      map.get(r.parentId)!.children.push(n);
    }
  });
  return roots;
}

async function loadCourses() {
  const { data } = await fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
  }
}

async function load() {
  if (!courseId.value) {
    tree.value = [];
    return;
  }
  setLastCourseId(courseId.value);
  loading.value = true;
  try {
    const { data } = await fetchKnowledgeList(courseId.value);
    tree.value = buildTree(data ?? []);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadCourses();
  await load();
});
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
