<template>
  <el-card>
    <template #header>题库浏览</template>
    <div class="bar">
      <el-select v-model="courseId" placeholder="课程" filterable style="width: 220px" @change="onCourseChange">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filterKp" clearable placeholder="知识点" style="width: 180px" @change="load">
        <el-option v-for="k in kpList" :key="k.id" :label="k.name" :value="k.id" />
      </el-select>
      <el-select v-model="filterType" clearable placeholder="题型" style="width: 140px" @change="load">
        <el-option label="单选" value="SINGLE" />
        <el-option label="多选" value="MULTIPLE" />
        <el-option label="判断" value="TRUE_FALSE" />
        <el-option label="填空" value="FILL" />
        <el-option label="简答" value="SHORT" />
      </el-select>
      <el-input v-model="keyword" placeholder="题干关键词" clearable style="width: 180px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="type" label="题型" width="100" />
      <el-table-column prop="stem" label="题干" min-width="220" show-overflow-tooltip />
      <el-table-column prop="scoreDefault" label="默认分值" width="96" />
      <el-table-column prop="reviewStatus" label="审核" width="100" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />

    <el-drawer v-model="detailVisible" title="试题详情" size="520px" destroy-on-close>
      <template v-if="detail">
        <p><strong>题型：</strong>{{ detail.type }}</p>
        <p><strong>题干：</strong></p>
        <div class="stem">{{ detail.stem }}</div>
        <p v-if="detail.optionsJson"><strong>选项：</strong></p>
        <pre v-if="detail.optionsJson" class="json">{{ detail.optionsJson }}</pre>
        <p><strong>答案：</strong>{{ detail.answer }}</p>
        <p v-if="detail.analysis"><strong>解析：</strong>{{ detail.analysis }}</p>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import { fetchKnowledgeList, type KpRow } from '@/api/knowledge';
import { fetchQuestionPage, getQuestion } from '@/api/modules/question';
import { getLastCourseId, setLastCourseId } from '@/composables/useLastCourseId';

const courses = ref<CourseRow[]>([]);
const courseId = ref<number | null>(getLastCourseId());
const kpList = ref<KpRow[]>([]);
const filterKp = ref<number | undefined>();
const filterType = ref<string | undefined>();
const keyword = ref('');
const loading = ref(false);
const rows = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

const detailVisible = ref(false);
const detail = ref<Record<string, string | number | null> | null>(null);

async function loadCourses() {
  const { data } = await fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
  }
}

async function onCourseChange() {
  filterKp.value = undefined;
  kpList.value = [];
  if (courseId.value) {
    setLastCourseId(courseId.value);
    const { data } = await fetchKnowledgeList(courseId.value);
    kpList.value = data ?? [];
  }
  await load();
}

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchQuestionPage({
      courseId: courseId.value,
      knowledgePointId: filterKp.value,
      type: filterType.value,
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    });
    rows.value = (data as { records?: Record<string, unknown>[] })?.records ?? [];
    total.value = (data as { total?: number })?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

async function openDetail(id: number) {
  detailVisible.value = true;
  detail.value = null;
  const { data } = await getQuestion(id);
  detail.value = data as Record<string, string | number | null>;
}

onMounted(async () => {
  await loadCourses();
  await onCourseChange();
  await load();
});
</script>

<style scoped>
.bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.stem {
  white-space: pre-wrap;
  line-height: 1.6;
  margin-bottom: 12px;
}
.json {
  font-size: 12px;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  overflow: auto;
}
</style>
