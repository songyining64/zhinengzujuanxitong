<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="head">
        <span>课程预览</span>
        <el-button @click="$router.push('/course/browse')">返回列表</el-button>
      </div>
    </template>
    <template v-if="course">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="课程名称">{{ course.name }}</el-descriptions-item>
        <el-descriptions-item label="课程代码">{{ course.code || '—' }}</el-descriptions-item>
        <el-descriptions-item label="简介">{{ course.description || '—' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="课程不存在或无权查看" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { getCourse, type CourseRow } from '@/api/course';

const route = useRoute();
const loading = ref(true);
const course = ref<CourseRow | null>(null);

const id = computed(() => Number(route.params.id));

async function load() {
  loading.value = true;
  try {
    const { data } = await getCourse(id.value);
    course.value = data;
  } catch {
    course.value = null;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
