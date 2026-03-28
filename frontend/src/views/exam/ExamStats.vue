<template>
  <el-card>
    <template v-if="isStudent">
      <p class="hint">选择已参加且成绩已发布的考试查看统计。</p>
      <el-select
        v-model="examId"
        filterable
        placeholder="选择考试"
        style="width: 360px; margin-bottom: 16px"
        @change="loadStats"
      >
        <el-option v-for="e in studentExams" :key="e.id" :label="`${e.courseName} - ${e.title}`" :value="e.id" />
      </el-select>
    </template>
    <template v-else>
      <div class="toolbar">
        <span class="label">课程</span>
        <el-select v-model="courseId" filterable style="width: 200px" @change="onCourseChange">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <span class="label">考试</span>
        <el-select
          v-model="examId"
          filterable
          placeholder="选择考试"
          style="width: 280px"
          @change="loadStats"
        >
          <el-option v-for="e in teacherExams" :key="e.id" :label="e.title" :value="e.id" />
        </el-select>
      </div>
    </template>

    <el-empty v-if="!examId" description="请选择考试" />

    <div v-else v-loading="loading">
      <el-descriptions v-if="overview" title="概览" :column="2" border class="block">
        <el-descriptions-item label="应交/已交">{{ overview.submittedCount }} / {{ overview.totalRecords }}</el-descriptions-item>
        <el-descriptions-item label="平均分">{{ overview.avgScore }}</el-descriptions-item>
        <el-descriptions-item label="最高分">{{ overview.maxScore }}</el-descriptions-item>
        <el-descriptions-item label="最低分">{{ overview.minScore }}</el-descriptions-item>
        <el-descriptions-item label="及格线">{{ overview.passScore ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="及格人数">{{ overview.passCount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="成绩已发布">{{ overview.scorePublished ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="mt">题目统计</h4>
      <el-table :data="qStats" size="small" stripe>
        <el-table-column prop="questionId" label="题目ID" width="88" />
        <el-table-column prop="type" label="题型" width="90" />
        <el-table-column prop="attemptCount" label="作答人次" width="100" />
        <el-table-column prop="correctCount" label="正确人次" width="100" />
        <el-table-column prop="correctRate" label="正确率" />
      </el-table>

      <h4 class="mt" v-if="!isStudent">排名（前 20）</h4>
      <el-table v-if="!isStudent" :data="rankRows" size="small" stripe>
        <el-table-column prop="rank" label="名次" width="64" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="totalScore" label="得分" width="88" />
      </el-table>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import http from '@/api/http';

interface CourseBrief {
  id: number;
  name: string;
}

interface ExamBrief {
  id: number;
  title: string;
}

interface StudentExamOpt {
  id: number;
  courseName: string;
  title: string;
}

const role = () => localStorage.getItem('role') || '';
const isStudent = computed(() => role() === 'STUDENT');

const courses = ref<CourseBrief[]>([]);
const courseId = ref<number | undefined>(undefined);
const teacherExams = ref<ExamBrief[]>([]);
const studentExams = ref<StudentExamOpt[]>([]);

const examId = ref<number | undefined>(undefined);
const loading = ref(false);
const overview = ref<Record<string, unknown> | null>(null);
const qStats = ref<Record<string, unknown>[]>([]);
const rankRows = ref<Record<string, unknown>[]>([]);

const loadCourses = async () => {
  const { data } = await http.get<{ records: CourseBrief[] }>('/api/course', { params: { page: 1, size: 500 } });
  courses.value = data?.records ?? [];
};

const loadTeacherExams = async () => {
  if (!courseId.value) {
    teacherExams.value = [];
    return;
  }
  const { data } = await http.get<{ records: ExamBrief[] }>('/api/exam/teacher', {
    params: { courseId: courseId.value, page: 1, size: 200 }
  });
  teacherExams.value = data?.records ?? [];
};

const loadStudentExams = async () => {
  const { data } = await http.get<{ records: StudentExamOpt[] }>('/api/exam/student', {
    params: { page: 1, size: 200 }
  });
  studentExams.value = data?.records ?? [];
};

const onCourseChange = async () => {
  examId.value = undefined;
  overview.value = null;
  await loadTeacherExams();
};

const loadStats = async () => {
  if (!examId.value) return;
  loading.value = true;
  try {
    const [{ data: ov }, { data: qs }, rankRes] = await Promise.all([
      http.get(`/api/exam/analytics/overview`, { params: { examId: examId.value } }),
      http.get(`/api/exam/analytics/question-stats`, { params: { examId: examId.value } }),
      isStudent.value
        ? Promise.resolve({ data: { records: [] as Record<string, unknown>[] } })
        : http.get<{ records: Record<string, unknown>[] }>('/api/exam/analytics/rank', {
            params: { examId: examId.value, page: 1, size: 20 }
          })
    ]);
    overview.value = (ov as Record<string, unknown>) ?? null;
    qStats.value = (qs as Record<string, unknown>[]) ?? [];
    rankRows.value = rankRes.data?.records ?? [];
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  if (isStudent.value) {
    await loadStudentExams();
  } else {
    await loadCourses();
    if (courses.value.length) {
      courseId.value = courses.value[0].id;
      await loadTeacherExams();
    }
  }
});
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.label {
  color: #606266;
  font-size: 14px;
}
.block {
  margin-bottom: 16px;
}
.mt {
  margin: 16px 0 8px;
  font-size: 15px;
}
.hint {
  color: #606266;
  margin-bottom: 8px;
}
</style>
