<template>
  <div class="dashboard">
    <header class="dash-head">
      <h1 class="dash-title">欢迎使用课程智能组卷系统</h1>
      <p class="role-line">
        <span class="hint-label">当前角色</span>
        <el-tag v-if="roleText" type="success" effect="light" class="role-tag">
          {{ roleText }}
        </el-tag>
      </p>
    </header>

    <el-skeleton v-if="statsLoading" :rows="3" animated class="stat-skeleton" />
    <div v-else class="stat-row">
      <div
        v-for="s in statCards"
        :key="s.key"
        class="stat-card"
        :class="'stat-card--' + s.tone"
      >
        <div :class="['stat-icon', s.tone]">
          <el-icon :size="24">
            <component :is="s.icon" />
          </el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ formatStat(s.value) }}</div>
        </div>
      </div>
    </div>

    <section class="quick-section">
      <h2 class="section-title">快速开始</h2>
      <div :class="['quick-grid', isStudent && 'quick-grid--student']">
        <div
          v-for="q in quickStartCards"
          :key="q.path + q.title"
          class="quick-card"
          role="button"
          tabindex="0"
          @click="go(q.path)"
          @keydown.enter="go(q.path)"
        >
          <div :class="['quick-icon', q.tone]">
            <el-icon :size="28">
              <component :is="q.icon" />
            </el-icon>
          </div>
          <div class="quick-body">
            <div class="quick-title">{{ q.title }}</div>
            <p class="quick-desc">{{ q.desc }}</p>
            <el-button :type="q.btnType" round @click.stop="go(q.path)">
              {{ q.btnText }}
            </el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import {
  Clock,
  Collection,
  DataAnalysis,
  DataLine,
  Document,
  MagicStick,
  Notebook,
  Reading,
  TrendCharts,
  User
} from '@element-plus/icons-vue';
import { fetchCoursePage } from '@/api/modules/course';
import { fetchStudentExams, fetchTeacherExams } from '@/api/modules/exam';
import { fetchPaperPage } from '@/api/modules/paper';
import { fetchQuestionPage } from '@/api/modules/question';
import { fetchWrongPage } from '@/api/modules/wrongbook';
import { useUserStore } from '@/store/user';
import { isRole } from '@/composables/usePermission';

const router = useRouter();
const { role } = storeToRefs(useUserStore());

const roleText = computed(() => {
  const m: Record<string, string> = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' };
  return m[role.value ?? ''] || role.value || '';
});

const isStudent = computed(() => isRole('STUDENT'));
const isTeacherLike = computed(() => isRole('TEACHER', 'ADMIN'));
const isAdmin = computed(() => isRole('ADMIN'));

const statsLoading = ref(false);

const statCourses = ref(0);
const statQuestions = ref(0);
const statPapers = ref(0);
const statExams = ref(0);

const statStudentExams = ref(0);
const statWrongEntries = ref(0);
const statMyCourses = ref(0);

const statCards = computed(() => {
  if (isStudent.value) {
    return [
      {
        key: 'se',
        label: '可参加考试',
        value: statStudentExams.value,
        icon: DataLine,
        tone: 'blue'
      },
      {
        key: 'wb',
        label: '错题收录',
        value: statWrongEntries.value,
        icon: Collection,
        tone: 'green'
      },
      {
        key: 'c',
        label: '我的课程',
        value: statMyCourses.value,
        icon: Reading,
        tone: 'amber'
      },
      {
        key: 'h',
        label: '学习提醒',
        value: '认真作答',
        icon: Clock,
        tone: 'violet'
      }
    ];
  }
  if (isTeacherLike.value) {
    return [
      {
        key: 'c',
        label: '我的课程',
        value: statCourses.value,
        icon: Reading,
        tone: 'blue'
      },
      {
        key: 'q',
        label: '题目总数',
        value: statQuestions.value,
        icon: Collection,
        tone: 'green'
      },
      {
        key: 'p',
        label: '试卷总数',
        value: statPapers.value,
        icon: Document,
        tone: 'amber'
      },
      {
        key: 'e',
        label: '考试场次',
        value: statExams.value,
        icon: TrendCharts,
        tone: 'violet'
      }
    ];
  }
  return [
    { key: 'r', label: '角色', value: roleText.value || '—', icon: DataLine, tone: 'blue' },
    { key: 'h', label: '提示', value: '使用侧栏导航', icon: Reading, tone: 'green' },
    { key: 'x', label: '状态', value: '就绪', icon: TrendCharts, tone: 'amber' },
    { key: 'y', label: '帮助', value: '见文档', icon: Clock, tone: 'violet' }
  ];
});

type QuickCard = {
  title: string;
  desc: string;
  path: string;
  icon: typeof MagicStick;
  tone: string;
  btnText: string;
  btnType: 'primary' | 'success' | 'warning' | 'danger' | 'info';
};

const quickStartCards = computed<QuickCard[]>(() => {
  if (isStudent.value) {
    return [
      {
        title: '我的考试',
        desc: '查看待考与已完成考试，进入答题。',
        path: '/exam/take',
        icon: Notebook,
        tone: 'blue',
        btnText: '去考试',
        btnType: 'primary'
      },
      {
        title: '错题本',
        desc: '按课程查看错题，巩固薄弱点。',
        path: '/wrongbook',
        icon: Collection,
        tone: 'amber',
        btnText: '打开错题本',
        btnType: 'warning'
      },
      {
        title: '课程浏览',
        desc: '浏览已选课程与教学资源。',
        path: '/course/browse',
        icon: Reading,
        tone: 'green',
        btnText: '浏览课程',
        btnType: 'success'
      },
      {
        title: '试卷浏览',
        desc: '查看已发布试卷与说明。',
        path: '/paper/browse',
        icon: Document,
        tone: 'violet',
        btnText: '浏览试卷',
        btnType: 'info'
      }
    ];
  }
  if (isAdmin.value) {
    return [
      {
        title: '用户管理',
        desc: '维护教师、学生账号与启用状态。',
        path: '/system/user',
        icon: User,
        tone: 'blue',
        btnText: '前往管理',
        btnType: 'primary'
      },
      {
        title: '智能组卷',
        desc: '基于算法与约束自动生成优质试卷。',
        path: '/paper/manage',
        icon: MagicStick,
        tone: 'violet',
        btnText: '开始组卷',
        btnType: 'primary'
      },
      {
        title: '课程管理',
        desc: '创建和管理课程与知识点。',
        path: '/course/manage',
        icon: Reading,
        tone: 'green',
        btnText: '前往管理',
        btnType: 'success'
      },
      {
        title: '题目管理',
        desc: '浏览、筛选与维护题库中的全部题目。',
        path: '/question/manage',
        icon: Collection,
        tone: 'amber',
        btnText: '前往管理',
        btnType: 'warning'
      }
    ];
  }
  if (isTeacherLike.value) {
    return [
      {
        title: '智能组卷',
        desc: '基于算法与约束自动生成优质试卷。',
        path: '/paper/manage',
        icon: MagicStick,
        tone: 'violet',
        btnText: '开始组卷',
        btnType: 'primary'
      },
      {
        title: '课程管理',
        desc: '创建和管理您的课程与知识点。',
        path: '/course/manage',
        icon: Reading,
        tone: 'green',
        btnText: '前往管理',
        btnType: 'success'
      },
      {
        title: '题目管理',
        desc: '浏览、筛选与维护题库中的全部题目。',
        path: '/question/manage',
        icon: Collection,
        tone: 'blue',
        btnText: '前往管理',
        btnType: 'primary'
      },
      {
        title: '数据分析',
        desc: '查看考试数据与统计分析。',
        path: '/exam/analytics',
        icon: DataAnalysis,
        tone: 'amber',
        btnText: '查看分析',
        btnType: 'warning'
      }
    ];
  }
  return [];
});

function go(path: string) {
  router.push(path);
}

function formatStat(v: number | null | string | undefined) {
  if (v === null || v === undefined) return '—';
  if (typeof v === 'string') return v;
  return v.toLocaleString();
}

async function loadTeacherStats() {
  if (!isTeacherLike.value) return;
  statsLoading.value = true;
  try {
    const coursePage = await fetchCoursePage({ page: 1, size: 500 });
    const records = coursePage.records ?? [];
    statCourses.value = coursePage.total ?? records.length;
    if (!records.length) {
      statQuestions.value = 0;
      statPapers.value = 0;
      statExams.value = 0;
      return;
    }
    let q = 0;
    let p = 0;
    let e = 0;
    await Promise.all(
      records.map(async (c) => {
        const [qp, pp, ep] = await Promise.all([
          fetchQuestionPage({ courseId: c.id, page: 1, size: 1 }),
          fetchPaperPage({ courseId: c.id, page: 1, size: 1 }),
          fetchTeacherExams(c.id, 1, 1)
        ]);
        q += qp.total;
        p += pp.total;
        e += ep.total;
      })
    );
    statQuestions.value = q;
    statPapers.value = p;
    statExams.value = e;
  } catch {
    statCourses.value = 0;
    statQuestions.value = 0;
    statPapers.value = 0;
    statExams.value = 0;
  } finally {
    statsLoading.value = false;
  }
}

async function loadStudentStats() {
  if (!isStudent.value) return;
  statsLoading.value = true;
  try {
    const [coursePage, examPage] = await Promise.all([
      fetchCoursePage({ page: 1, size: 1 }),
      fetchStudentExams(1, 200)
    ]);
    statMyCourses.value = coursePage?.total ?? 0;
    statStudentExams.value = examPage.total ?? 0;
    const courseIds = [
      ...new Set((examPage.records ?? []).map((r) => r.courseId).filter(Boolean))
    ];
    let wrongSum = 0;
    const slice = courseIds.slice(0, 12);
    await Promise.all(
      slice.map(async (cid) => {
        try {
          const wp = await fetchWrongPage(cid, 1, 1);
          wrongSum += wp.total ?? 0;
        } catch {
          /* 忽略单课程无权限 */
        }
      })
    );
    statWrongEntries.value = wrongSum;
  } catch {
    statMyCourses.value = 0;
    statStudentExams.value = 0;
    statWrongEntries.value = 0;
  } finally {
    statsLoading.value = false;
  }
}

onMounted(() => {
  if (isTeacherLike.value) {
    loadTeacherStats();
  } else if (isStudent.value) {
    loadStudentStats();
  } else {
    statsLoading.value = false;
  }
});
</script>

<style scoped>
.dashboard {
  max-width: 1120px;
  margin: 0 auto;
}

.dash-head {
  margin-bottom: 22px;
}

.dash-title {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 700;
  color: #1d4ed8;
  letter-spacing: 0.02em;
}

.role-line {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hint-label {
  font-size: 14px;
  color: #64748b;
}

.role-tag {
  font-weight: 600;
}

.stat-skeleton {
  margin-bottom: 28px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 32px;
}

@media (max-width: 960px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 18px 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 14px rgba(15, 23, 42, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.25);
  transition:
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.stat-card:hover {
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.stat-body {
  min-width: 0;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.blue {
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
}
.stat-icon.green {
  background: rgba(22, 163, 74, 0.12);
  color: #16a34a;
}
.stat-icon.amber {
  background: rgba(217, 119, 6, 0.12);
  color: #d97706;
}
.stat-icon.violet {
  background: rgba(124, 58, 237, 0.12);
  color: #7c3aed;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
}

.quick-section {
  margin-bottom: 24px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.quick-grid--student {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 960px) {
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.quick-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 22px 20px;
  text-align: left;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 14px;
  cursor: pointer;
  box-shadow: 0 2px 14px rgba(15, 23, 42, 0.05);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.quick-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 32px rgba(37, 99, 235, 0.12);
}

.quick-card:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.quick-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-icon.blue {
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
}
.quick-icon.green {
  background: rgba(22, 163, 74, 0.12);
  color: #16a34a;
}
.quick-icon.amber {
  background: rgba(217, 119, 6, 0.12);
  color: #d97706;
}
.quick-icon.violet {
  background: rgba(124, 58, 237, 0.14);
  color: #7c3aed;
}

.quick-body {
  flex: 1;
  min-width: 0;
}

.quick-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.quick-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 14px;
}

.quick-body :deep(.el-button) {
  font-weight: 600;
}
</style>
