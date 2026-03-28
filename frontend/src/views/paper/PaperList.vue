<template>
  <el-card>
    <div class="toolbar">
      <span class="label">课程</span>
      <el-select v-model="courseId" filterable placeholder="课程" style="width: 220px" @change="fetchData">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button v-if="canManage && courseId" type="primary" style="margin-left: auto" @click="openAuto">
        自动组卷
      </el-button>
    </div>

    <el-empty v-if="!courseId" description="请先选择课程" />
    <el-table v-else v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="mode" label="模式" width="100" />
      <el-table-column prop="totalScore" label="总分" width="88" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="canManage" link type="danger" @click="removePaper(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="courseId"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="fetchData"
      @size-change="fetchData"
    />

    <el-dialog v-model="autoVisible" title="自动组卷" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="试卷标题" required>
          <el-input v-model="autoForm.title" />
        </el-form-item>
        <el-form-item label="单选题数量">
          <el-input-number v-model="autoForm.nSingle" :min="0" :max="99" />
        </el-form-item>
        <el-form-item label="多选题数量">
          <el-input-number v-model="autoForm.nMulti" :min="0" :max="99" />
        </el-form-item>
        <el-form-item label="判断题数量">
          <el-input-number v-model="autoForm.nTf" :min="0" :max="99" />
        </el-form-item>
        <el-form-item label="随机种子">
          <el-input-number v-model="autoForm.randomSeed" :min="0" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoVisible = false">取消</el-button>
        <el-button type="primary" :loading="autoLoading" @click="submitAuto">生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="试卷详情" size="560px">
      <div v-if="paperDetail">
        <p><strong>标题</strong>：{{ paperDetail.paper?.title }}</p>
        <p><strong>模式</strong>：{{ paperDetail.paper?.mode }} / <strong>总分</strong>：{{ paperDetail.paper?.totalScore }}</p>
        <el-table :data="paperDetail.questions || []" size="small" stripe class="mt">
          <el-table-column prop="questionOrder" label="#" width="48" />
          <el-table-column prop="type" label="题型" width="88" />
          <el-table-column prop="score" label="分值" width="72" />
          <el-table-column label="题干" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.stem?.slice(0, 120) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import http from '@/api/http';

interface CourseBrief {
  id: number;
  name: string;
}

interface PaperRow {
  id: number;
  title: string;
  mode?: string;
  totalScore?: number;
}

interface PaperDetail {
  paper?: { title?: string; mode?: string; totalScore?: number };
  questions?: { questionOrder: number; type: string; score: number; stem: string }[];
}

const role = () => localStorage.getItem('role') || '';
const canManage = computed(() => role() === 'ADMIN' || role() === 'TEACHER');

const courses = ref<CourseBrief[]>([]);
const courseId = ref<number | undefined>(undefined);
const page = ref(1);
const size = ref(10);
const total = ref(0);
const list = ref<PaperRow[]>([]);
const loading = ref(false);

const autoVisible = ref(false);
const autoLoading = ref(false);
const autoForm = reactive({ title: '', nSingle: 5, nMulti: 0, nTf: 0, randomSeed: undefined as number | undefined });

const detailVisible = ref(false);
const paperDetail = ref<PaperDetail | null>(null);

const loadCourses = async () => {
  const { data } = await http.get<{ records: CourseBrief[] }>('/api/course', { params: { page: 1, size: 500 } });
  courses.value = data?.records ?? [];
};

const fetchData = async () => {
  if (!courseId.value) {
    list.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const { data } = await http.get<{ records: PaperRow[]; total: number }>('/api/paper', {
      params: { courseId: courseId.value, page: page.value, size: size.value }
    });
    list.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
};

const openAuto = () => {
  autoForm.title = `试卷-${new Date().toLocaleString()}`;
  autoForm.nSingle = 5;
  autoForm.nMulti = 0;
  autoForm.nTf = 0;
  autoForm.randomSeed = Date.now() % 100000;
  autoVisible.value = true;
};

const submitAuto = async () => {
  if (!courseId.value || !autoForm.title.trim()) return;
  const countByType: Record<string, number> = {};
  if (autoForm.nSingle > 0) countByType.SINGLE = autoForm.nSingle;
  if (autoForm.nMulti > 0) countByType.MULTIPLE = autoForm.nMulti;
  if (autoForm.nTf > 0) countByType.TRUE_FALSE = autoForm.nTf;
  if (!Object.keys(countByType).length) return;
  autoLoading.value = true;
  try {
    await http.post('/api/paper/auto-generate', {
      courseId: courseId.value,
      title: autoForm.title,
      countByType,
      randomSeed: autoForm.randomSeed,
      dedup: true,
      randomPool: false,
      includeKnowledgeDescendants: true
    });
    autoVisible.value = false;
    await fetchData();
  } finally {
    autoLoading.value = false;
  }
};

const openDetail = async (row: PaperRow) => {
  const { data } = await http.get<PaperDetail>(`/api/paper/${row.id}`);
  paperDetail.value = data ?? null;
  detailVisible.value = true;
};

const removePaper = async (row: PaperRow) => {
  await ElMessageBox.confirm('确定删除该试卷？', '提示', { type: 'warning' });
  await http.delete(`/api/paper/${row.id}`);
  await fetchData();
};

onMounted(async () => {
  await loadCourses();
  if (courses.value.length) {
    courseId.value = courses.value[0].id;
    await fetchData();
  }
});
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.label {
  color: #606266;
  font-size: 14px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.mt {
  margin-top: 12px;
}
</style>
