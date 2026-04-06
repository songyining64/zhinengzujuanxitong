<template>
  <div class="dash-zujuan">
    <header class="dash-head">
      <h1 class="dash-title">仪表盘</h1>
      <p class="dash-sub">欢迎使用课程智能组卷系统</p>
    </header>

    <section class="stat-row" v-loading="statsLoading" aria-label="数据概览">
      <div v-for="s in statCards" :key="s.key" class="stat-card">
        <div class="stat-icon" :class="s.tone">
          <el-icon :size="26"><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ formatStat(s.value) }}</div>
        </div>
      </div>
    </section>

    <section class="quick-section">
      <h2 class="section-title">快速操作</h2>
      <div class="quick-grid" :class="{ 'quick-grid-compact': quickActions.length < 4 }">
        <button
          v-for="q in quickActions"
          :key="q.title"
          type="button"
          class="quick-card"
          @click="q.path && $router.push(q.path)"
        >
          <div class="quick-icon" :class="q.tone">
            <el-icon :size="28"><component :is="q.icon" /></el-icon>
          </div>
          <div class="quick-text">
            <div class="quick-title">{{ q.title }}</div>
            <div class="quick-desc">{{ q.desc }}</div>
          </div>
        </button>
      </div>
    </section>

    <el-card v-if="isTeacherLike" shadow="never" class="role-hint">
      <div class="hint-row">
        <span class="hint-label">当前角色</span>
        <el-tag size="small" type="info">{{ roleText }}</el-tag>
        <span class="hint-muted">侧边栏已按组卷网常用流程整理；更多入口见「更多功能」。</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import {
  CirclePlus,
  Clock,
  Collection,
  DataLine,
  Document,
  MagicStick,
  Reading,
  TrendCharts
} from '@element-plus/icons-vue';
import http from '@/api/http';
import { fetchQuestionPage } from '@/api/modules/question';
import { fetchPaperPage } from '@/api/modules/paper';
import { useUserStore } from '@/store/user';
import { isRole } from '@/composables/usePermission';

const { role } = storeToRefs(useUserStore());

const roleText = computed(() => {
  const m: Record<string, string> = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' };
  return m[role.value] || role.value || '—';
});

const isStudent = computed(() => isRole('STUDENT'));
const isTeacherLike = computed(() => isRole('TEACHER', 'ADMIN'));

const statsLoading = ref(false);
const statQuestions = ref(0);
const statPapers = ref(0);

const statCards = computed(() => {
  if (isStudent.value) {
    return [
      { key: 'sq', label: '我的考试', value: null, icon: Reading, tone: 'tone-blue' },
      { key: 'wb', label: '错题本', value: null, icon: Collection, tone: 'tone-green' },
      { key: 'w1', label: '本周考试', value: '—', icon: TrendCharts, tone: 'tone-amber' },
      { key: 'w2', label: '学习提醒', value: '—', icon: Clock, tone: 'tone-violet' }
    ];
  }
  return [
    { key: 'q', label: '总题目数', value: statQuestions.value, icon: Collection, tone: 'tone-blue' },
    { key: 'p', label: '总试卷数', value: statPapers.value, icon: Document, tone: 'tone-green' },
    { key: 'wq', label: '本周新增题目', value: '—', icon: TrendCharts, tone: 'tone-amber' },
    { key: 'wp', label: '本周新增试卷', value: '—', icon: Clock, tone: 'tone-violet' }
  ];
});

const quickActions = computed(() => {
  if (isStudent.value) {
    return [
      {
        title: '我的考试',
        desc: '查看待考与已完成试卷。',
        path: '/exam/take',
        icon: DataLine,
        tone: 'tone-blue'
      },
      {
        title: '错题本',
        desc: '巩固错题，查漏补缺。',
        path: '/wrongbook',
        icon: Collection,
        tone: 'tone-amber'
      }
    ];
  }
  if (!isTeacherLike.value) {
    return [];
  }
  return [
    {
      title: '新增题目',
      desc: '向题库录入新试题。',
      path: '/question/manage?action=new',
      icon: CirclePlus,
      tone: 'tone-blue'
    },
    {
      title: '智能组卷',
      desc: '按知识点与题型自动组卷。',
      path: '/paper/manage',
      icon: MagicStick,
      tone: 'tone-green'
    },
    {
      title: '人工组卷',
      desc: '手动选题编排试卷。',
      path: '/paper',
      icon: Document,
      tone: 'tone-violet'
    },
    {
      title: '题目管理',
      desc: '浏览、审核与维护全部题目。',
      path: '/question/manage',
      icon: Collection,
      tone: 'tone-amber'
    }
  ];
});

function formatStat(v: number | null | string | undefined) {
  if (v === null || v === undefined) return '—';
  if (typeof v === 'string') return v;
  return v.toLocaleString();
}

async function loadTeacherStats() {
  if (!isTeacherLike.value) return;
  statsLoading.value = true;
  try {
    const { data } = await http.get<{ records: { id: number }[] }>('/api/course', {
      params: { page: 1, size: 40 }
    });
    const records = data?.records ?? [];
    if (!records.length) {
      statQuestions.value = 0;
      statPapers.value = 0;
      return;
    }
    let q = 0;
    let p = 0;
    await Promise.all(
      records.map(async (c) => {
        const [qp, pp] = await Promise.all([
          fetchQuestionPage({ courseId: c.id, page: 1, size: 1 }),
          fetchPaperPage({ courseId: c.id, page: 1, size: 1 })
        ]);
        q += qp.total;
        p += pp.total;
      })
    );
    statQuestions.value = q;
    statPapers.value = p;
  } catch {
    statQuestions.value = 0;
    statPapers.value = 0;
  } finally {
    statsLoading.value = false;
  }
}

onMounted(() => {
  loadTeacherStats();
});
</script>

<style scoped>
.dash-zujuan {
  max-width: 1120px;
}

.dash-head {
  margin-bottom: 22px;
}

.dash-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.02em;
}

.dash-sub {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 28px;
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
  border: 1px solid rgba(52, 152, 219, 0.1);
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

.stat-icon.tone-blue {
  background: rgba(52, 152, 219, 0.15);
  color: var(--primary);
}
.stat-icon.tone-green {
  background: rgba(39, 174, 96, 0.15);
  color: #27ae60;
}
.stat-icon.tone-amber {
  background: rgba(230, 126, 34, 0.14);
  color: #e67e22;
}
.stat-icon.tone-violet {
  background: rgba(155, 89, 182, 0.14);
  color: #9b59b6;
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
  margin-bottom: 22px;
}

.section-title {
  margin: 0 0 14px;
  font-size: 17px;
  font-weight: 600;
  color: #1e293b;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

@media (max-width: 960px) {
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.quick-grid-compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-width: 720px;
}

.quick-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  text-align: left;
  background: #fff;
  border: 1px solid rgba(52, 152, 219, 0.12);
  border-radius: 14px;
  cursor: pointer;
  box-shadow: 0 2px 14px rgba(15, 23, 42, 0.05);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.quick-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(52, 152, 219, 0.12);
}

.quick-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-icon.tone-blue {
  background: rgba(52, 152, 219, 0.14);
  color: var(--primary);
}
.quick-icon.tone-green {
  background: rgba(39, 174, 96, 0.14);
  color: #27ae60;
}
.quick-icon.tone-amber {
  background: rgba(230, 126, 34, 0.14);
  color: #e67e22;
}
.quick-icon.tone-violet {
  background: rgba(155, 89, 182, 0.14);
  color: #9b59b6;
}

.quick-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.quick-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
}

.role-hint {
  border-radius: 12px;
}

.hint-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.hint-label {
  color: #64748b;
}

.hint-muted {
  color: #94a3b8;
}
</style>
