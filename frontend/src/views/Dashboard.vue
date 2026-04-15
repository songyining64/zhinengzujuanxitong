<template>
  <div class="dashboard-page">
    <div class="welcome-card">
      <h1 class="welcome-title">欢迎使用课程智能组卷系统</h1>
      <p class="role-line">
        <span class="role-label">当前角色：</span>
        <span class="role-badge">{{ roleText }}</span>
      </p>
    </div>

    <section class="quick-block">
      <h2 class="section-heading">
        <span class="section-bar" aria-hidden="true" />
        快速开始
      </h2>
      <div class="cards-grid">
        <article
          v-for="card in quickCards"
          :key="card.key"
          class="feature-card"
          :class="card.tone"
        >
          <div class="card-icon-wrap">
            <el-icon class="card-icon"><component :is="card.icon" /></el-icon>
          </div>
          <h3 class="card-title">{{ card.title }}</h3>
          <p class="card-desc">{{ card.desc }}</p>
          <el-button class="card-btn" @click="router.push(card.path)">{{ card.btn }}</el-button>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { Collection, DataAnalysis, MagicStick, Reading, Tickets } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/user';
import { isRole } from '@/composables/usePermission';

const router = useRouter();
const { role } = storeToRefs(useUserStore());

const roleText = computed(() => {
  const m: Record<string, string> = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' };
  return m[role.value || ''] || role.value || '—';
});

const isStudent = computed(() => isRole('STUDENT'));
const isTeacherLike = computed(() => isRole('TEACHER', 'ADMIN'));

const quickCards = computed(() => {
  if (isStudent.value) {
    return [
      {
        key: 'exam',
        title: '我的考试',
        desc: '查看待考与已完成试卷，进入答题。',
        path: '/exam/take',
        btn: '进入考试',
        icon: Tickets,
        tone: 'tone-purple'
      },
      {
        key: 'wrong',
        title: '错题本',
        desc: '巩固错题，查漏补缺。',
        path: '/wrongbook',
        btn: '查看错题',
        icon: Collection,
        tone: 'tone-green'
      },
      {
        key: 'course',
        title: '课程浏览',
        desc: '浏览已选课程与学习内容。',
        path: '/course/browse',
        btn: '浏览课程',
        icon: Reading,
        tone: 'tone-blue'
      },
      {
        key: 'qb',
        title: '题库浏览',
        desc: '浏览和筛选课程题库资源。',
        path: '/question/browse',
        btn: '查看题库',
        icon: Collection,
        tone: 'tone-orange'
      }
    ];
  }
  if (isTeacherLike.value) {
    return [
      {
        key: 'paper',
        title: '智能组卷',
        desc: '基于AI算法自动生成优质试卷。',
        path: '/paper/manage',
        btn: '开始组卷',
        icon: MagicStick,
        tone: 'tone-purple'
      },
      {
        key: 'course',
        title: '课程管理',
        desc: '创建和管理您的课程与知识点。',
        path: '/course/manage',
        btn: '前往管理',
        icon: Reading,
        tone: 'tone-green'
      },
      {
        key: 'qm',
        title: '题目管理',
        desc: '浏览、筛选、审核与维护题库中的全部题目，与侧栏「题目管理」一致。',
        path: '/question/manage',
        btn: '前往管理',
        icon: Collection,
        tone: 'tone-blue'
      },
      {
        key: 'analytics',
        title: '数据分析',
        desc: '查看考试数据和统计分析。',
        path: '/exam/analytics',
        btn: '查看分析',
        icon: DataAnalysis,
        tone: 'tone-orange'
      }
    ];
  }
  return [
    {
      key: 'course',
      title: '课程浏览',
      desc: '浏览系统中的公开课程。',
      path: '/course/browse',
      btn: '前往浏览',
      icon: Reading,
      tone: 'tone-green'
    },
    {
      key: 'qb',
      title: '题库浏览',
      desc: '浏览题库资源。',
      path: '/question/browse',
      btn: '查看题库',
      icon: Collection,
      tone: 'tone-blue'
    }
  ];
});
</script>

<style scoped>
.dashboard-page {
  /* 与 main-shell 内边距抵消，使仪表盘灰底铺满内容区 */
  margin: -22px -26px -36px;
  padding: 24px 26px 40px;
  min-height: calc(100vh - 120px);
  background: #f0f2f5;
  box-sizing: border-box;
}

.welcome-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
  margin-bottom: 24px;
}

.welcome-title {
  margin: 0 0 16px;
  font-size: 26px;
  font-weight: 700;
  color: #1565c0;
  letter-spacing: 0.02em;
  line-height: 1.3;
}

.role-line {
  margin: 0;
  font-size: 15px;
  color: #606266;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.role-label {
  color: #606266;
}

.role-badge {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: #66bb6a;
  line-height: 1.4;
}

.quick-block {
  padding: 0 4px;
}

.section-heading {
  margin: 0 0 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.section-bar {
  width: 4px;
  height: 20px;
  border-radius: 2px;
  background: #3498db;
  flex-shrink: 0;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 992px) {
  .cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .cards-grid {
    grid-template-columns: 1fr;
  }
}

/* 第四张卡片单独占下一行首列（与设计图 3+1 一致） */
.cards-grid .feature-card:nth-child(4) {
  grid-column: 1;
}

@media (max-width: 992px) {
  .cards-grid .feature-card:nth-child(4) {
    grid-column: auto;
  }
}

.feature-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 22px 22px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
  border-top: 3px solid var(--card-accent, #5c6bc0);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-height: 220px;
  box-sizing: border-box;
}

.feature-card.tone-purple {
  --card-accent: #5c6bc0;
  --icon-bg: #5c6bc0;
}
.feature-card.tone-green {
  --card-accent: #66bb6a;
  --icon-bg: #66bb6a;
}
.feature-card.tone-blue {
  --card-accent: #42a5f5;
  --icon-bg: #42a5f5;
}
.feature-card.tone-orange {
  --card-accent: #ffa726;
  --icon-bg: #ffa726;
}

.card-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: var(--icon-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.card-icon {
  font-size: 28px;
  color: #fff;
}

.card-title {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.card-desc {
  margin: 0 0 auto;
  padding-bottom: 18px;
  font-size: 14px;
  color: #909399;
  line-height: 1.55;
  flex: 1;
}

.card-btn {
  width: 100%;
  max-width: 200px;
  background: #fff !important;
  border: 1px solid #dcdfe6 !important;
  color: #606266 !important;
  font-weight: 500;
}

.card-btn:hover {
  border-color: var(--card-accent) !important;
  color: var(--card-accent) !important;
}
</style>
