<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>题库管理</span>
        <el-button type="primary" :disabled="!courseId" @click="openCreate">新增试题</el-button>
      </div>
    </template>
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
      <el-input v-model="keyword" placeholder="题干关键词" clearable style="width: 160px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="type" label="题型" width="100" />
      <el-table-column prop="stem" label="题干" min-width="200" show-overflow-tooltip />
      <el-table-column prop="reviewStatus" label="审核" width="100" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="dlg" :title="editId ? '编辑试题' : '新增试题'" width="640px" destroy-on-close @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="知识点" required>
          <el-select v-model="form.knowledgePointId" placeholder="选择" filterable style="width: 100%">
            <el-option v-for="k in kpList" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="单选" value="SINGLE" />
            <el-option label="多选" value="MULTIPLE" />
            <el-option label="判断" value="TRUE_FALSE" />
            <el-option label="填空" value="FILL" />
            <el-option label="简答" value="SHORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="form.stem" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="选项 JSON">
          <el-input v-model="form.optionsJson" type="textarea" :rows="3" placeholder='客观题示例：["A","B","C","D"]' />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="form.answer" placeholder="单选/判断填字母；多选逗号分隔" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="默认分值">
          <el-input-number v-model="form.scoreDefault" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="难度">
          <el-input-number v-model="form.difficulty" :min="1" :max="3" />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="form.reviewStatus" style="width: 100%">
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待审" value="PENDING" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import { fetchKnowledgeList, type KpRow } from '@/api/knowledge';
import { fetchQuestionPage, getQuestion, createQuestion, updateQuestion, deleteQuestion } from '@/api/modules/question';
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

const dlg = ref(false);
const editId = ref<number | null>(null);
const saving = ref(false);
const form = reactive({
  knowledgePointId: undefined as number | undefined,
  type: 'SINGLE',
  stem: '',
  optionsJson: '',
  answer: '',
  analysis: '',
  scoreDefault: 10,
  difficulty: 2,
  reviewStatus: 'PUBLISHED'
});

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

function resetForm() {
  editId.value = null;
  form.knowledgePointId = kpList.value[0]?.id;
  form.type = 'SINGLE';
  form.stem = '';
  form.optionsJson = '';
  form.answer = '';
  form.analysis = '';
  form.scoreDefault = 10;
  form.difficulty = 2;
  form.reviewStatus = 'PUBLISHED';
}

function openCreate() {
  resetForm();
  dlg.value = true;
}

async function openEdit(row: Record<string, unknown>) {
  editId.value = row.id as number;
  const { data } = await getQuestion(editId.value);
  const d = data as Record<string, unknown>;
  form.knowledgePointId = d.knowledgePointId as number;
  form.type = String(d.type);
  form.stem = String(d.stem ?? '');
  form.optionsJson = String(d.optionsJson ?? '');
  form.answer = String(d.answer ?? '');
  form.analysis = String(d.analysis ?? '');
  form.scoreDefault = Number(d.scoreDefault ?? 10);
  form.difficulty = Number(d.difficulty ?? 2);
  form.reviewStatus = String(d.reviewStatus ?? 'PUBLISHED');
  dlg.value = true;
}

async function save() {
  if (!courseId.value || !form.knowledgePointId || !form.stem.trim() || !form.answer.trim()) return;
  saving.value = true;
  try {
    const payload: Record<string, unknown> = {
      courseId: courseId.value,
      knowledgePointId: form.knowledgePointId,
      type: form.type,
      stem: form.stem,
      optionsJson: form.optionsJson || undefined,
      answer: form.answer,
      analysis: form.analysis || undefined,
      scoreDefault: form.scoreDefault,
      difficulty: form.difficulty,
      reviewStatus: form.reviewStatus
    };
    if (editId.value) {
      await updateQuestion(editId.value, payload);
    } else {
      await createQuestion(payload);
    }
    dlg.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function onDelete(row: Record<string, unknown>) {
  try {
    await ElMessageBox.confirm('确定删除该试题？', '删除', { type: 'warning' });
  } catch {
    return;
  }
  await deleteQuestion(row.id as number);
  await load();
}

onMounted(async () => {
  await loadCourses();
  await onCourseChange();
  await load();
});
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
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
</style>
