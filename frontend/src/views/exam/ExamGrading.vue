<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <span>主观题阅卷</span>
      </template>
      <p class="hint">仅处理试卷中含「简答题」且学生已交卷、尚未打分的题目。打分后系统自动汇总该场考试记录总分。</p>

      <div class="filters">
        <el-select v-model="courseId" placeholder="课程" filterable style="width: 260px" @change="onCourseChange">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="examId" placeholder="考试" filterable style="width: 320px" @change="loadPending">
          <el-option v-for="e in exams" :key="e.id" :label="e.title" :value="e.id" />
        </el-select>
        <el-button type="primary" :disabled="!examId" @click="loadPending">刷新待阅卷</el-button>
      </div>

      <el-table v-loading="loading" :data="pending" stripe class="mt">
        <el-table-column label="考生" min-width="140">
          <template #default="{ row }">
            {{ row.studentRealName || row.studentUsername || row.studentId }}
          </template>
        </el-table-column>
        <el-table-column prop="examTitle" label="考试" min-width="120" show-overflow-tooltip />
        <el-table-column label="题干" min-width="220">
          <template #default="{ row }">
            <div class="stem-preview">{{ row.stem }}</div>
          </template>
        </el-table-column>
        <el-table-column label="作答" min-width="200">
          <template #default="{ row }">
            <span class="answer-preview">{{ row.userAnswer || '（空）' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="满分" width="80">
          <template #default="{ row }">{{ row.maxScore }}</template>
        </el-table-column>
        <el-table-column label="得分" width="160" fixed="right">
          <template #default="{ row }">
            <el-input-number
              v-model="scores[keyOf(row)]"
              :min="0"
              :max="row.maxScore"
              :step="0.5"
              :precision="2"
              size="small"
              controls-position="right"
            />
            <el-button type="primary" size="small" class="ml" :loading="savingKey === keyOf(row)" @click="submit(row)">
              提交
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && examId && pending.length === 0" description="暂无待阅卷简答题" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import * as courseApi from '@/api/modules/course';
import * as examApi from '@/api/modules/exam';
import * as gradingApi from '@/api/modules/grading';
import type { SubjectivePending } from '@/api/modules/grading';

const courses = ref<courseApi.Course[]>([]);
const exams = ref<examApi.Exam[]>([]);
const courseId = ref<number | undefined>();
const examId = ref<number | undefined>();
const loading = ref(false);
const pending = ref<SubjectivePending[]>([]);
const scores = reactive<Record<string, number>>({});
const savingKey = ref<string | null>(null);

function keyOf(row: SubjectivePending) {
  return `${row.examRecordId}-${row.questionId}`;
}

async function boot() {
  const data = await courseApi.fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
}

async function onCourseChange() {
  examId.value = undefined;
  pending.value = [];
  if (!courseId.value) {
    exams.value = [];
    return;
  }
  const data = await examApi.fetchTeacherExams(courseId.value, 1, 100);
  exams.value = data?.records ?? [];
}

async function loadPending() {
  if (!examId.value) return;
  loading.value = true;
  try {
    const list = await gradingApi.fetchPendingSubjective(examId.value);
    pending.value = list;
    for (const row of list) {
      const k = keyOf(row);
      if (scores[k] === undefined) {
        scores[k] = 0;
      }
    }
  } finally {
    loading.value = false;
  }
}

async function submit(row: SubjectivePending) {
  const k = keyOf(row);
  const score = scores[k];
  if (score === undefined || score < 0) {
    ElMessage.warning('请填写得分');
    return;
  }
  savingKey.value = k;
  try {
    await gradingApi.submitSubjectiveScore({
      examRecordId: row.examRecordId,
      questionId: row.questionId,
      score
    });
    ElMessage.success('已保存');
    await loadPending();
  } catch {
    /* http */
  } finally {
    savingKey.value = null;
  }
}

onMounted(boot);
</script>

<style scoped>
.page {
  max-width: 1280px;
}
.hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.mt {
  margin-top: 16px;
}
.stem-preview,
.answer-preview {
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
  word-break: break-word;
}
.ml {
  margin-left: 8px;
  vertical-align: middle;
}
</style>
