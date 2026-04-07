<template>
  <div class="exam-manage-page">
    <div class="exam-manage-card">
      <header class="card-profile">
        <div class="profile-left">
          <el-avatar :size="52" class="profile-avatar">
            {{ avatarLetter }}
          </el-avatar>
          <div class="profile-text">
            <div class="profile-line1">个人信息</div>
            <div class="profile-line2">{{ username || '演示用户' }} · 校园</div>
          </div>
        </div>
        <el-button type="primary" round class="btn-logout" @click="logout">退出</el-button>
      </header>

      <div class="card-toolbar">
        <CoursePicker />
        <el-button type="primary" :disabled="!courseId" @click="openForm(null)">创建考试</el-button>
      </div>

      <el-alert
        v-if="!courseId"
        type="info"
        show-icon
        :closable="false"
        title="请先在上方选择课程"
        class="need-course"
      />

      <template v-else>
        <section class="list-block">
          <h3 class="block-title">待参加考试列表</h3>
          <div v-loading="loading" class="table-wrap">
            <table class="exam-table">
              <thead>
                <tr>
                  <th>考试名称</th>
                  <th>课程</th>
                  <th>开始时间</th>
                  <th>时长</th>
                  <th>操作按钮</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in upcomingExams" :key="row.id">
                  <td>{{ row.title }}</td>
                  <td>{{ courseName(row.courseId) }}</td>
                  <td>{{ formatDt(row.startTime) }}</td>
                  <td>{{ row.durationMinutes }} 分钟</td>
                  <td class="cell-actions">
                    <el-dropdown trigger="click" @command="(cmd: string) => onUpcomingCommand(cmd, row)">
                      <el-button type="primary" plain round>操作</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="edit">编辑</el-dropdown-item>
                          <el-dropdown-item command="publish" v-if="row.status !== 'PUBLISHED'">发布</el-dropdown-item>
                          <el-dropdown-item command="end" divided>结束</el-dropdown-item>
                          <el-dropdown-item command="publishScore">公布成绩</el-dropdown-item>
                          <el-dropdown-item command="unpublishScore">撤销成绩</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </td>
                </tr>
                <tr v-if="!loading && !upcomingExams.length">
                  <td colspan="5" class="empty-cell">暂无待参加考试</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="list-block">
          <h3 class="block-title">已完成考试列表</h3>
          <div v-loading="loading" class="table-wrap">
            <table class="exam-table">
              <thead>
                <tr>
                  <th>考试名称</th>
                  <th>课程</th>
                  <th>开始时间</th>
                  <th>时长</th>
                  <th>操作按钮</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in completedExams" :key="row.id">
                  <td>{{ row.title }}</td>
                  <td>{{ courseName(row.courseId) }}</td>
                  <td>{{ formatDt(row.startTime) }}</td>
                  <td>{{ row.durationMinutes }} 分钟</td>
                  <td class="cell-actions">
                    <el-dropdown trigger="click" @command="(cmd: string) => onCompletedCommand(cmd, row)">
                      <el-button type="primary" plain round>操作</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="edit">编辑</el-dropdown-item>
                          <el-dropdown-item command="publishScore">公布成绩</el-dropdown-item>
                          <el-dropdown-item command="unpublishScore">撤销成绩</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </td>
                </tr>
                <tr v-if="!loading && !completedExams.length">
                  <td colspan="5" class="empty-cell">暂无已完成考试</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </div>

    <el-dialog v-model="dlg.visible" :title="dlg.id ? '编辑考试' : '创建考试'" width="560px" destroy-on-close>
      <el-form :model="dlg.form" label-width="120px">
        <el-form-item label="试卷 ID" required>
          <el-input-number v-model="dlg.form.paperId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="dlg.form.title" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="dlg.form.description" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="dlg.form.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="dlg.form.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="考试时长(分)" required>
          <el-input-number v-model="dlg.form.durationMinutes" :min="1" />
        </el-form-item>
        <el-form-item label="及格分">
          <el-input-number v-model="dlg.form.passScore" :min="0" />
        </el-form-item>
        <el-form-item label="题目乱序">
          <el-switch v-model="dlg.form.shuffleQuestions" />
        </el-form-item>
        <el-form-item label="选项乱序">
          <el-switch v-model="dlg.form.shuffleOptions" />
        </el-form-item>
        <el-form-item label="切屏上限">
          <el-input-number v-model="dlg.form.switchBlurLimit" :min="0" placeholder="0 不限制" />
        </el-form-item>
        <el-form-item label="公布成绩">
          <el-switch v-model="dlg.form.scorePublished" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import CoursePicker from '@/components/CoursePicker.vue';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchCoursePage } from '@/api/modules/course';
import {
  fetchTeacherExams,
  createExam,
  updateExam,
  publishExam,
  endExam,
  publishExamScore,
  unpublishExamScore
} from '@/api/modules/examTeacher';
import { mergeCoursesWithSamples, isSampleCourseId } from '@/data/sampleCourses';
import type { Course } from '@/types/models';
import type { ExamCreateRequest, ExamEntity } from '@/types/examTeacher';

const router = useRouter();
const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

const username = computed(() => localStorage.getItem('username') || '');
const avatarLetter = computed(() => (username.value || '用').slice(0, 1).toUpperCase());

const coursesList = ref<Course[]>([]);
const courseMap = computed(() => {
  const m = new Map<number, Course>();
  for (const c of coursesList.value) m.set(c.id, c);
  return m;
});

function courseName(cid: number) {
  const c = courseMap.value.get(cid);
  if (c) return isSampleCourseId(c.id) ? `${c.name}（示例）` : c.name;
  return `课程 #${cid}`;
}

async function loadCourses() {
  try {
    const { data } = await fetchCoursePage(1, 500);
    coursesList.value = mergeCoursesWithSamples(data?.records ?? []);
  } catch {
    coursesList.value = mergeCoursesWithSamples([]);
  }
}

onMounted(() => {
  void loadCourses();
});

const loading = ref(false);
const list = ref<ExamEntity[]>([]);
const FETCH_SIZE = 500;

const upcomingExams = computed(() => list.value.filter((e) => e.status !== 'ENDED'));
const completedExams = computed(() => list.value.filter((e) => e.status === 'ENDED'));

function formatDt(s: string) {
  return s?.replace('T', ' ').slice(0, 16) || '—';
}

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchTeacherExams(courseId.value, 1, FETCH_SIZE);
    list.value = data?.records ?? [];
  } finally {
    loading.value = false;
  }
}

watch(courseId, () => {
  list.value = [];
  if (courseId.value) void load();
}, { immediate: true });

function logout() {
  localStorage.removeItem('access_token');
  localStorage.removeItem('username');
  localStorage.removeItem('role');
  router.push('/login');
}

function defaultForm(): ExamCreateRequest {
  return {
    courseId: courseId.value!,
    paperId: 1,
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    durationMinutes: 90,
    passScore: 60,
    scorePublished: false,
    shuffleQuestions: true,
    shuffleOptions: true,
    switchBlurLimit: 0
  };
}

const dlg = reactive({
  visible: false,
  id: null as number | null,
  saving: false,
  form: defaultForm()
});

function openForm(row: ExamEntity | null) {
  if (!courseId.value) return;
  if (row) {
    dlg.id = row.id;
    dlg.form = {
      courseId: row.courseId,
      paperId: row.paperId,
      title: row.title,
      description: row.description || '',
      startTime: row.startTime,
      endTime: row.endTime,
      durationMinutes: row.durationMinutes,
      passScore: row.passScore != null ? Number(row.passScore) : undefined,
      scorePublished: !!row.scorePublished,
      shuffleQuestions: row.shuffleQuestions !== 0,
      shuffleOptions: row.shuffleOptions !== 0,
      switchBlurLimit: row.switchBlurLimit ?? 0
    };
  } else {
    dlg.id = null;
    dlg.form = defaultForm();
  }
  dlg.visible = true;
}

async function save() {
  if (!courseId.value) return;
  dlg.form.courseId = courseId.value;
  if (!dlg.form.title?.trim() || !dlg.form.paperId || !dlg.form.startTime || !dlg.form.endTime) {
    ElMessage.warning('请完善必填项');
    return;
  }
  dlg.saving = true;
  try {
    const body = {
      ...dlg.form,
      switchBlurLimit: dlg.form.switchBlurLimit || null
    };
    if (dlg.id) {
      await updateExam(dlg.id, body);
      ElMessage.success('已更新');
    } else {
      await createExam(body);
      ElMessage.success('已创建');
    }
    dlg.visible = false;
    await load();
  } finally {
    dlg.saving = false;
  }
}

async function onUpcomingCommand(cmd: string, row: ExamEntity) {
  if (cmd === 'edit') openForm(row);
  else if (cmd === 'publish') {
    await publishExam(row.id);
    ElMessage.success('已发布');
    await load();
  } else if (cmd === 'end') {
    await endExam(row.id);
    ElMessage.success('已结束');
    await load();
  } else if (cmd === 'publishScore') {
    await publishExamScore(row.id);
    ElMessage.success('成绩已公布');
    await load();
  } else if (cmd === 'unpublishScore') {
    await unpublishExamScore(row.id);
    ElMessage.success('已撤销成绩发布');
    await load();
  }
}

async function onCompletedCommand(cmd: string, row: ExamEntity) {
  if (cmd === 'edit') openForm(row);
  else if (cmd === 'publishScore') {
    await publishExamScore(row.id);
    ElMessage.success('成绩已公布');
    await load();
  } else if (cmd === 'unpublishScore') {
    await unpublishExamScore(row.id);
    ElMessage.success('已撤销成绩发布');
    await load();
  }
}
</script>

<style scoped>
/* 与布局主内容区一致：不铺渐变，沿用 DefaultLayout .main 的 #f5f7fa */
.exam-manage-page {
  max-width: 1040px;
  margin: 0 auto;
}

.exam-manage-card {
  background: #fff;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: none;
  padding: 20px 20px 24px;
  box-sizing: border-box;
}

.card-profile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 20px;
}

.profile-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  font-weight: 600;
  font-size: 18px;
}

.profile-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-line1 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.profile-line2 {
  font-size: 13px;
  color: #909399;
}

.btn-logout {
  padding: 10px 28px;
  font-weight: 500;
}

.card-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.need-course {
  margin-bottom: 16px;
}

.list-block + .list-block {
  margin-top: 32px;
}

.block-title {
  margin: 0 0 14px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.table-wrap {
  min-height: 80px;
  border-radius: 8px;
  overflow: hidden;
}

.exam-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  color: #303133;
}

.exam-table thead th {
  background: #d9ecff;
  color: #303133;
  font-weight: 600;
  text-align: center;
  padding: 14px 12px;
  border: none;
  border-bottom: 1px solid #c6e2ff;
}

.exam-table tbody td {
  text-align: center;
  padding: 16px 12px;
  border: none;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  vertical-align: middle;
}

.exam-table tbody tr:last-child td {
  border-bottom: none;
}

.exam-table tbody tr:hover td {
  background: #fafcff;
}

.cell-actions {
  text-align: center !important;
}

.empty-cell {
  color: #c0c4cc;
  padding: 28px !important;
  text-align: center !important;
}

@media (max-width: 768px) {
  .exam-manage-card {
    padding: 16px;
  }

  .exam-table {
    display: block;
    overflow-x: auto;
    white-space: nowrap;
  }

  .card-profile {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
