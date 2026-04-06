<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span class="card-title">题库浏览</span>
      </div>
    </template>

    <div class="toolbar">
      <div class="toolbar-row">
        <span class="label">课程</span>
        <el-select
          v-model="selectedCourseId"
          placeholder="请选择课程"
          filterable
          clearable
          style="width: 260px"
          @change="onCourseChange"
        >
          <el-option v-for="c in courseList" :key="c.id" :label="courseLabel(c)" :value="c.id" />
        </el-select>

        <span class="label" style="margin-left: 16px">知识点</span>
        <el-tree-select
          v-model="selectedKpId"
          :data="kpTree"
          :props="{ label: 'name', children: 'children', value: 'id' }"
          clearable
          filterable
          placeholder="全部知识点"
          style="width: 260px"
          :disabled="!selectedCourseId"
          @change="onFilterChange"
        />
      </div>

      <div class="toolbar-row">
        <span class="label">题型</span>
        <el-select
          v-model="query.type"
          placeholder="全部"
          clearable
          style="width: 160px"
          @change="onFilterChange"
        >
          <el-option label="全部" :value="undefined" />
          <el-option label="单选题" value="SINGLE" />
          <el-option label="多选题" value="MULTIPLE" />
          <el-option label="判断题" value="TRUE_FALSE" />
          <el-option label="填空题" value="FILL" />
          <el-option label="简答题" value="SHORT" />
        </el-select>

        <span class="label" style="margin-left: 16px">审核状态</span>
        <el-select
          v-model="query.reviewStatus"
          placeholder="全部"
          clearable
          style="width: 160px"
          @change="onFilterChange"
        >
          <el-option label="全部" :value="undefined" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>

        <el-input
          v-model="query.keyword"
          placeholder="搜索题干关键字"
          clearable
          style="width: 260px; margin-left: 16px"
          @keyup.enter="fetchQuestions"
        />
        <el-button type="primary" style="margin-left: 8px" :disabled="!selectedCourseId" @click="fetchQuestions">
          搜索
        </el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </div>

    <el-table
      :data="list"
      stripe
      v-loading="loading"
      :row-key="(row: QuestionRow) => row.id"
      style="margin-top: 8px"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="knowledgePointId" label="知识点ID" width="100" />
      <el-table-column prop="type" label="题型" width="96">
        <template #default="{ row }">
          {{ typeLabel(row.type) }}
        </template>
      </el-table-column>
      <el-table-column label="题干" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.stem }}
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="80" />
      <el-table-column prop="reviewStatus" label="审核状态" width="100">
        <template #default="{ row }">
          <el-tag :type="reviewTagType(row.reviewStatus)" size="small">
            {{ reviewLabel(row.reviewStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="170" />
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchQuestions"
        @size-change="fetchQuestions"
      />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import http from '@/api/http';
import { fetchQuestionPage } from '@/api/modules/question';

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

interface QuestionRow {
  id: number;
  courseId: number;
  knowledgePointId: number;
  type: string;
  stem: string;
  difficulty?: number;
  reviewStatus?: string;
  updateTime?: string;
}

interface QuestionPage {
  records: QuestionRow[];
  total: number;
}

const courseList = ref<CourseRow[]>([]);
const selectedCourseId = ref<number | undefined>(undefined);
const kpTree = ref<KpRow[]>([]);
const selectedKpId = ref<number | undefined>(undefined);

const query = reactive({
  type: undefined as string | undefined,
  keyword: '',
  reviewStatus: undefined as string | undefined
});

const list = ref<QuestionRow[]>([]);
const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);

function courseLabel(c: CourseRow) {
  if (c.code) return `${c.name}（${c.code}）`;
  return c.name;
}

function typeLabel(t: string | undefined) {
  switch (t) {
    case 'SINGLE':
      return '单选';
    case 'MULTIPLE':
      return '多选';
    case 'TRUE_FALSE':
      return '判断';
    case 'FILL':
      return '填空';
    case 'SHORT':
      return '简答';
    default:
      return t || '-';
  }
}

function reviewLabel(t: string | undefined) {
  switch (t) {
    case 'DRAFT':
      return '草稿';
    case 'PENDING':
      return '待审核';
    case 'PUBLISHED':
      return '已发布';
    case 'REJECTED':
      return '已驳回';
    default:
      return t || '-';
  }
}

function reviewTagType(t: string | undefined) {
  switch (t) {
    case 'DRAFT':
      return 'info';
    case 'PENDING':
      return 'warning';
    case 'PUBLISHED':
      return 'success';
    case 'REJECTED':
      return 'danger';
    default:
      return '';
  }
}

function buildKpTree(items: KpRow[]): KpRow[] {
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
  const { data } = await http.get<CoursePage>('/api/course', {
    params: { page: 1, size: 200 }
  });
  courseList.value = data?.records ?? [];
  if (!selectedCourseId.value && courseList.value.length) {
    selectedCourseId.value = courseList.value[0].id;
  }
}

async function fetchKnowledgePoints() {
  if (!selectedCourseId.value) {
    kpTree.value = [];
    return;
  }
  const { data } = await http.get<KpRow[]>('/api/knowledge-point', {
    params: { courseId: selectedCourseId.value }
  });
  const list = (data ?? []).map((k) => ({
    ...k,
    parentId: k.parentId ?? null
  }));
  kpTree.value = buildKpTree(list);
}

const fetchQuestions = async () => {
  if (!selectedCourseId.value) {
    list.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const p = await fetchQuestionPage({
      courseId: selectedCourseId.value,
      knowledgePointId: selectedKpId.value,
      type: query.type || undefined,
      keyword: query.keyword.trim() || undefined,
      reviewStatus: query.reviewStatus || undefined,
      page: page.value,
      size: size.value
    });
    list.value = p?.records ?? [];
    total.value = p?.total ?? 0;
  } catch {
    ElMessage.error('加载题库失败');
  } finally {
    loading.value = false;
  }
};

function resetFilters() {
  query.type = undefined;
  query.keyword = '';
  query.reviewStatus = undefined;
  selectedKpId.value = undefined;
  page.value = 1;
  fetchQuestions();
}

function onFilterChange() {
  page.value = 1;
  fetchQuestions();
}

function onCourseChange() {
  selectedKpId.value = undefined;
  page.value = 1;
  fetchKnowledgePoints();
  fetchQuestions();
}

onMounted(async () => {
  await fetchCourses();
  await fetchKnowledgePoints();
  await fetchQuestions();
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
  margin-top: 8px;
  margin-bottom: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.label {
  font-size: 14px;
  color: #606266;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

