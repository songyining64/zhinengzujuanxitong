<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <span>手工组卷</span>
        <CoursePicker />
      </div>
    </template>

    <el-alert v-if="!courseId" type="info" show-icon :closable="false" title="请先选择课程" />

    <template v-else>
      <el-form :inline="true" class="filters">
        <el-form-item label="题型">
          <el-select v-model="filterType" clearable placeholder="全部" style="width: 120px">
            <el-option label="单选" value="SINGLE" />
            <el-option label="多选" value="MULTIPLE" />
            <el-option label="判断" value="TRUE_FALSE" />
            <el-option label="填空" value="FILL" />
            <el-option label="简答" value="SHORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="keyword" clearable style="width: 160px" @keyup.enter="loadQuestions" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadQuestions">加载试题</el-button>
        </el-form-item>
      </el-form>

      <div class="title-row">
        <span class="lbl">试卷标题</span>
        <el-input v-model="title" placeholder="请输入试卷标题" style="max-width: 400px" />
      </div>

      <el-row :gutter="16">
        <el-col :span="14">
          <h3 class="sub">可选试题</h3>
          <el-table
            ref="tableRef"
            v-loading="qLoading"
            :data="questions"
            height="420"
            @selection-change="onSelAvailable"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="type" label="题型" width="88" />
            <el-table-column prop="stem" label="题干" show-overflow-tooltip />
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="page"
              layout="prev, pager, next"
              :total="total"
              :page-size="size"
              @current-change="loadQuestions"
            />
          </div>
        </el-col>
        <el-col :span="10">
          <h3 class="sub">已选题目（拖拽 ⋮⋮ 排序）</h3>
          <div class="picked-wrap">
            <draggable v-model="picked" item-key="questionId" handle=".drag-handle" class="drag-list">
              <template #item="{ element }">
                <div class="pick-item">
                  <span class="drag-handle" title="拖动排序">⋮⋮</span>
                  <div class="pick-body">
                    <div class="pick-id">#{{ element.questionId }}</div>
                    <div class="pick-stem">{{ element.stem }}</div>
                    <el-input-number v-model="element.score" :min="1" :step="1" size="small" class="pick-score" />
                    <el-button link type="danger" size="small" @click="removePicked(element.questionId)">移除</el-button>
                  </div>
                </div>
              </template>
            </draggable>
          </div>
          <div class="footer-actions">
            <el-button
              type="primary"
              :disabled="!selectedBuffer.length"
              @click="addSelected"
            >
              添加选中 →
            </el-button>
            <el-button type="success" :loading="saving" :disabled="!picked.length || !title.trim()" @click="save">
              生成试卷
            </el-button>
          </div>
        </el-col>
      </el-row>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import draggable from 'vuedraggable';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import CoursePicker from '@/components/CoursePicker.vue';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchQuestionPage } from '@/api/modules/question';
import { createManualPaper } from '@/api/modules/paper';
import type { Question } from '@/types/models';

const router = useRouter();
const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

const filterType = ref('');
const keyword = ref('');
const title = ref('');

const qLoading = ref(false);
const questions = ref<Question[]>([]);
const page = ref(1);
const size = ref(15);
const total = ref(0);
const selectedBuffer = ref<Question[]>([]);
const tableRef = ref();

const picked = ref<{ questionId: number; stem: string; score: number }[]>([]);

watch(courseId, () => {
  questions.value = [];
  total.value = 0;
  picked.value = [];
  if (courseId.value) void loadQuestions();
}, { immediate: true });

async function loadQuestions() {
  if (!courseId.value) return;
  qLoading.value = true;
  try {
    const { data } = await fetchQuestionPage({
      courseId: courseId.value,
      type: filterType.value || undefined,
      keyword: keyword.value || undefined,
      reviewStatus: 'PUBLISHED',
      page: page.value,
      size: size.value
    });
    questions.value = data?.records ?? [];
    total.value = data?.total ?? 0;
  } finally {
    qLoading.value = false;
  }
}

function onSelAvailable(rows: Question[]) {
  selectedBuffer.value = rows;
}

function addSelected() {
  const exist = new Set(picked.value.map((p) => p.questionId));
  for (const q of selectedBuffer.value) {
    if (exist.has(q.id)) continue;
    exist.add(q.id);
    picked.value.push({
      questionId: q.id,
      stem: q.stem.slice(0, 80),
      score: Number(q.scoreDefault) || 5
    });
  }
  ElMessage.success('已加入组卷列表');
}

function removePicked(qid: number) {
  picked.value = picked.value.filter((p) => p.questionId !== qid);
}

const saving = ref(false);

async function save() {
  if (!courseId.value || !title.value.trim() || !picked.value.length) return;
  saving.value = true;
  try {
    const { data } = await createManualPaper({
      courseId: courseId.value,
      title: title.value.trim(),
      items: picked.value.map((p) => ({ questionId: p.questionId, score: p.score }))
    });
    ElMessage.success('已生成试卷');
    await router.push(`/paper/${data.id}`);
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filters {
  margin-bottom: 8px;
}

.title-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-row .lbl {
  font-size: 14px;
  color: #606266;
}

.sub {
  font-size: 14px;
  margin: 0 0 8px;
}

.pager {
  margin-top: 8px;
  display: flex;
  justify-content: center;
}

.footer-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.picked-wrap {
  max-height: 420px;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fafafa;
}

.drag-list {
  min-height: 40px;
}

.pick-item {
  display: flex;
  gap: 8px;
  padding: 8px 10px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  background: #fff;
  align-items: flex-start;
}

.drag-handle {
  cursor: grab;
  color: #909399;
  user-select: none;
  padding-top: 6px;
}

.pick-body {
  flex: 1;
  display: grid;
  grid-template-columns: 52px 1fr 100px auto;
  gap: 8px;
  align-items: start;
  font-size: 13px;
}

.pick-id {
  color: #909399;
}

.pick-stem {
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

@media (max-width: 768px) {
  .pick-body {
    grid-template-columns: 1fr;
  }
}
</style>
