<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>题库</span>
          <div>
            <el-button v-if="canRead" @click="downloadTemplate">下载导入模板</el-button>
            <el-upload
              v-if="canManage && courseId"
              class="inline-upload"
              :show-file-list="false"
              accept=".xlsx,.xls"
              :http-request="doImport"
            >
              <el-button type="success">Excel 导入</el-button>
            </el-upload>
            <el-button v-if="canManage && courseId" type="primary" @click="openCreate">新增试题</el-button>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-select v-model="courseId" placeholder="课程" filterable style="width: 240px" @change="load">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="qType" clearable placeholder="题型" style="width: 140px" @change="load">
          <el-option label="单选" value="SINGLE" />
          <el-option label="多选" value="MULTIPLE" />
          <el-option label="判断" value="TRUE_FALSE" />
          <el-option label="填空" value="FILL" />
          <el-option label="简答" value="SHORT" />
        </el-select>
        <el-input v-model="keyword" placeholder="题干关键词" clearable style="width: 200px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button v-if="canRead && courseId" @click="exportExcel">导出 Excel</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="type" label="题型" width="100" />
        <el-table-column prop="stem" label="题干" min-width="240" show-overflow-tooltip />
        <el-table-column prop="scoreDefault" label="默认分值" width="100" />
        <el-table-column v-if="canManage" label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="visible" :title="title" width="640px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="知识点" required>
          <el-select v-model="form.knowledgePointId" filterable style="width: 100%">
            <el-option v-for="k in knowledge" :key="k.id" :label="k.name" :value="k.id" />
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
          <el-input v-model="form.stem" type="textarea" rows="4" />
        </el-form-item>
        <el-form-item label="选项 JSON">
          <el-input
            v-model="form.optionsJson"
            type="textarea"
            rows="3"
            placeholder='客观题填 JSON 数组，如 ["A","B","C"]'
          />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="form.answer" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="难度">
          <el-input-number v-model="form.difficulty" :min="1" :max="3" />
        </el-form-item>
        <el-form-item label="默认分值">
          <el-input-number v-model="form.scoreDefault" :min="0" :step="0.5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { UploadRequestOptions } from 'element-plus';
import * as qApi from '@/api/modules/question';
import type { Question } from '@/api/modules/question';
import * as courseApi from '@/api/modules/course';
import * as kpApi from '@/api/modules/knowledge';
import http from '@/api/http';
import { hasPerm } from '@/composables/usePermission';

const courses = ref<courseApi.Course[]>([]);
const courseId = ref<number | undefined>();
const knowledge = ref<kpApi.KnowledgePoint[]>([]);
const loading = ref(false);
const rows = ref<Question[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');
const qType = ref<string | undefined>();

const canManage = computed(() => hasPerm('question:manage'));
const canRead = computed(() => hasPerm('question:read'));

const visible = ref(false);
const title = ref('试题');
const editingId = ref<number | null>(null);
const saving = ref(false);
const form = reactive({
  knowledgePointId: undefined as number | undefined,
  type: 'SINGLE',
  stem: '',
  optionsJson: '',
  answer: '',
  analysis: '',
  difficulty: 1,
  scoreDefault: 5
});

async function boot() {
  const data = await courseApi.fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
}

async function loadKp() {
  if (!courseId.value) {
    knowledge.value = [];
    return;
  }
  knowledge.value = (await kpApi.listKnowledge(courseId.value)) ?? [];
}

async function load() {
  if (!courseId.value) {
    rows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const data = await qApi.fetchQuestionPage({
      courseId: courseId.value,
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      type: qType.value
    });
    rows.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = null;
  title.value = '新增试题';
  form.knowledgePointId = knowledge.value[0]?.id;
  form.type = 'SINGLE';
  form.stem = '';
  form.optionsJson = '["选项A","选项B","选项C","选项D"]';
  form.answer = 'A';
  form.analysis = '';
  form.difficulty = 1;
  form.scoreDefault = 5;
  visible.value = true;
}

function openEdit(row: Question) {
  editingId.value = row.id;
  title.value = '编辑试题';
  form.knowledgePointId = row.knowledgePointId;
  form.type = row.type;
  form.stem = row.stem;
  form.optionsJson = row.optionsJson || '';
  form.answer = row.answer;
  form.analysis = row.analysis || '';
  form.difficulty = row.difficulty ?? 1;
  form.scoreDefault = row.scoreDefault ?? 5;
  visible.value = true;
}

async function submit() {
  if (!courseId.value || !form.knowledgePointId || !form.stem.trim() || !form.answer.trim()) {
    ElMessage.warning('请完善必填项');
    return;
  }
  saving.value = true;
  try {
    const body = {
      courseId: courseId.value,
      knowledgePointId: form.knowledgePointId,
      type: form.type,
      stem: form.stem,
      optionsJson: form.optionsJson || undefined,
      answer: form.answer,
      analysis: form.analysis || undefined,
      difficulty: form.difficulty,
      scoreDefault: form.scoreDefault,
      reviewStatus: 'PUBLISHED'
    };
    if (editingId.value) {
      await qApi.updateQuestion(editingId.value, body);
    } else {
      await qApi.createQuestion(body);
    }
    ElMessage.success('已保存');
    visible.value = false;
    await load();
  } catch {
    /* */
  } finally {
    saving.value = false;
  }
}

async function onDelete(row: Question) {
  await ElMessageBox.confirm('确定删除该试题？', '确认', { type: 'warning' });
  await qApi.deleteQuestion(row.id);
  ElMessage.success('已删除');
  await load();
}

async function downloadBlob(path: string, filename: string) {
  const token = localStorage.getItem('access_token');
  const r = await fetch(path, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!r.ok) throw new Error('下载失败');
  const blob = await r.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

async function exportExcel() {
  if (!courseId.value) return;
  await downloadBlob(`/api/question/export?courseId=${courseId.value}`, 'questions.xlsx');
}

async function downloadTemplate() {
  await downloadBlob('/api/question/import/template', 'question-import-template.xlsx');
}

async function doImport(opt: UploadRequestOptions) {
  if (!courseId.value) return;
  const fd = new FormData();
  fd.append('file', opt.file as File);
  await http.post(`/api/question/import?courseId=${courseId.value}`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  ElMessage.success('导入完成');
  await load();
}

onMounted(async () => {
  await boot();
});

watch(courseId, async () => {
  page.value = 1;
  await loadKp();
  await load();
});
</script>

<style scoped>
.page {
  max-width: 1200px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.head > div {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.inline-upload {
  display: inline-block;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
