<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">知识点浏览</span>
      </div>
    </template>

    <div class="toolbar">
      <span class="label">课程</span>
      <el-select
        v-model="selectedCourseId"
        placeholder="请选择课程"
        filterable
        clearable
        style="width: 320px"
        @change="onCourseChange"
      >
        <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
      </el-select>
      <el-button type="primary" style="margin-left: 8px" :disabled="!selectedCourseId" @click="fetchKnowledgePoints">
        刷新
      </el-button>
    </div>

    <el-alert
      v-if="coursesLoaded && courseList.length === 0"
      type="warning"
      :closable="false"
      show-icon
      class="course-hint"
      title="暂无可用课程，请先在课程管理中创建课程或确认账号权限。"
    />

    <el-table
      v-if="selectedCourseId"
      :data="treeData"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="name" label="名称" min-width="260" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
    </el-table>
    <el-empty v-else description="请先选择课程" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import http from '@/api/http';

interface CourseRow {
  id: number;
  name: string;
  code?: string;
}

interface CoursePage {
  records: CourseRow[];
  total: number;
}

interface KpRow {
  id: number;
  courseId: number;
  parentId: number | null;
  name: string;
  sortOrder: number;
  createTime?: string;
  children?: KpRow[];
}

const coursesLoaded = ref(false);
const courseList = ref<CourseRow[]>([]);
const selectedCourseId = ref<number | undefined>(undefined);
const treeData = ref<KpRow[]>([]);

function courseLabel(c: CourseRow) {
  if (c.code) return `${c.name}（${c.code}）`;
  return c.name;
}

function buildTree(items: KpRow[]): KpRow[] {
  const map = new Map<number, KpRow>();
  items.forEach((i) => map.set(i.id, { ...i, children: [] }));
  const roots: KpRow[] = [];
  for (const i of items) {
    const node = map.get(i.id)!;
    const pid = i.parentId;
    if (pid == null) {
      roots.push(node);
      continue;
    }
    const parent = map.get(pid);
    if (parent) {
      parent.children!.push(node);
    } else {
      roots.push(node);
    }
  }
  const sortRec = (nodes: KpRow[]) => {
    nodes.sort((a, b) => (a.sortOrder - b.sortOrder) || a.id - b.id);
    nodes.forEach((n) => {
      if (n.children?.length) sortRec(n.children);
      else delete n.children;
    });
  };
  sortRec(roots);
  return roots;
}

async function fetchCourses() {
  coursesLoaded.value = false;
  try {
    const { data } = await http.get<CoursePage>('/api/course', { params: { page: 1, size: 200 } });
    courseList.value = data?.records ?? [];
    if (!selectedCourseId.value && courseList.value.length) {
      selectedCourseId.value = courseList.value[0].id;
    }
  } finally {
    coursesLoaded.value = true;
  }
}

async function fetchKnowledgePoints() {
  if (!selectedCourseId.value) {
    treeData.value = [];
    return;
  }
  const { data } = await http.get<KpRow[]>('/api/knowledge-point', {
    params: { courseId: selectedCourseId.value }
  });
  const list = (data ?? []).map((k) => ({
    ...k,
    parentId: k.parentId ?? null
  }));
  treeData.value = buildTree(list);
}

async function onCourseChange() {
  await fetchKnowledgePoints();
}

onMounted(async () => {
  await fetchCourses();
  await fetchKnowledgePoints();
});
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar .label {
  font-size: 14px;
  color: #606266;
}

.course-hint {
  margin-bottom: 16px;
}
</style>
