<template>
  <div class="dash">
    <el-row :gutter="20" class="row-welcome">
      <el-col :xs="24" :lg="15">
        <div class="hero-wrap">
          <div class="hero-main">
            <div class="hero-kicker">工作台</div>
            <h1 class="hero-title">课程智能组卷系统</h1>
            <p v-if="role" class="hero-role">
              当前身份
              <el-tag size="small" type="primary" effect="light" round>{{ role }}</el-tag>
            </p>
            <p class="hero-lead">{{ roleHint }}</p>
            <div class="quick-actions">
              <template v-if="isStudent">
                <el-button type="primary" size="large" round @click="$router.push('/wrong-book')">
                  <el-icon class="btn-ic"><DocumentCopy /></el-icon>
                  错题本
                </el-button>
                <el-button size="large" round @click="$router.push('/exam/analytics')">
                  <el-icon class="btn-ic"><TrendCharts /></el-icon>
                  成绩分析
                </el-button>
              </template>
              <template v-else>
                <el-button type="primary" size="large" round @click="$router.push('/course')">
                  <el-icon class="btn-ic"><Reading /></el-icon>
                  课程管理
                </el-button>
                <el-button size="large" round @click="$router.push('/question')">
                  <el-icon class="btn-ic"><Collection /></el-icon>
                  题库
                </el-button>
                <el-button size="large" round @click="$router.push('/paper/compose/auto')">
                  <el-icon class="btn-ic"><MagicStick /></el-icon>
                  智能组卷
                </el-button>
                <el-button size="large" round @click="$router.push('/exam/teacher')">
                  <el-icon class="btn-ic"><Notebook /></el-icon>
                  考试管理
                </el-button>
                <el-button size="large" round @click="$router.push('/exam/analytics')">
                  <el-icon class="btn-ic"><TrendCharts /></el-icon>
                  成绩分析
                </el-button>
              </template>
            </div>
          </div>
          <div class="hero-art" aria-hidden="true">
            <div class="hero-blob hero-blob-a" />
            <div class="hero-blob hero-blob-b" />
            <div class="hero-ring" />
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="9">
        <el-card class="calendar-shell" shadow="hover">
          <template #header>
            <div class="cal-head">
              <span class="cal-title">
                <el-icon><Calendar /></el-icon>
                教学活动日历
              </span>
              <el-tag size="small" effect="plain" type="info">本月安排</el-tag>
            </div>
          </template>
          <el-calendar v-model="calendarDay" class="dash-cal">
            <template #date-cell="{ data }">
              <div class="cal-cell" :class="{ 'is-today': isToday(data.day) }">
                <span class="cal-date">{{ dayNum(data.day) }}</span>
              </div>
            </template>
          </el-calendar>
        </el-card>
      </el-col>
    </el-row>

      <div class="section-label">
        <span>数据概览</span>
        <el-tag v-if="demoMode" size="small" type="warning" effect="light">演示数据</el-tag>
        <el-tag v-else size="small" type="success" effect="light">实时统计待接入</el-tag>
      </div>
    <el-row :gutter="20" class="row-stats">
      <el-col v-for="c in statCards" :key="c.key" :xs="12" :sm="12" :md="6">
        <div
          class="stat-card"
          :class="['tone-' + c.tone, { clickable: !!c.to }]"
          @click="c.to && $router.push(c.to)"
        >
          <div class="stat-icon-wrap" :class="'icon-' + c.tone">
            <el-icon :size="26"><component :is="c.icon" /></el-icon>
          </div>
          <div class="stat-text">
            <div class="stat-val">{{ c.value }}</div>
            <div class="stat-label">{{ c.label }}</div>
            <div class="stat-sub">{{ c.sub }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

      <div class="section-label section-spaced">功能导览</div>
    <el-row :gutter="20" class="row-tiles">
      <el-col :xs="24" :sm="12" :md="8">
        <div class="tile tile-a" @click="$router.push('/knowledge')">
          <div class="tile-icon"><el-icon :size="28"><Share /></el-icon></div>
          <h3>知识点</h3>
          <p>树形知识结构，划定组卷考查范围</p>
          <span class="tile-link">进入模块 →</span>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <div class="tile tile-b" @click="$router.push('/paper/template')">
          <div class="tile-icon"><el-icon :size="28"><Files /></el-icon></div>
          <h3>组卷模板</h3>
          <p>保存抽题策略，一键生成平行试卷</p>
          <span class="tile-link">进入模块 →</span>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <div class="tile tile-c" @click="$router.push('/exam/analytics')">
          <div class="tile-icon"><el-icon :size="28"><PieChart /></el-icon></div>
          <h3>分析图表</h3>
          <p>成绩分布、逐题得分与知识点薄弱项</p>
          <span class="tile-link">进入模块 →</span>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { Component } from 'vue';
import {
  Calendar,
  Collection,
  DataBoard,
  DataLine,
  DocumentCopy,
  Files,
  MagicStick,
  Notebook,
  PieChart,
  Reading,
  Share,
  Timer,
  TrendCharts,
  Warning
} from '@element-plus/icons-vue';
import { isDemoSession } from '@/constants/auth';

const role = computed(() => localStorage.getItem('role') || '');
const isStudent = computed(() => role.value === 'STUDENT');
const demoMode = computed(() => isDemoSession());

const roleHint = computed(() => {
  if (isStudent.value) return '浏览成绩分析、整理错题；可在右侧日历查看本月安排。';
  if (role.value === 'ADMIN')
    return '管理系统用户、审核入库题目，并查看全平台教学与考试数据。';
  return '维护课程与题库、配置智能组卷规则、发布考试并查看学情分析。';
});

const calendarDay = ref(new Date());

function dayNum(isoDay: string) {
  return isoDay.split('-').pop() || '';
}

function isToday(isoDay: string) {
  const d = new Date(isoDay);
  const t = new Date();
  return d.getFullYear() === t.getFullYear() && d.getMonth() === t.getMonth() && d.getDate() === t.getDate();
}

type Tone = 'blue' | 'green' | 'violet' | 'amber';

interface StatItem {
  key: string;
  label: string;
  value: string;
  sub: string;
  to?: string;
  tone: Tone;
  icon: Component;
}

const statCards = computed<StatItem[]>(() => {
  const suffix = demoMode.value ? '以下为演示数值' : '后端统计接口接入后刷新';

  if (isStudent.value) {
    return [
      {
        key: 'wb',
        label: '错题收录',
        value: '14',
        sub: suffix,
        to: '/wrong-book',
        tone: 'amber',
        icon: Warning
      },
      {
        key: 'st',
        label: '本周学习',
        value: '5',
        sub: '天 · ' + suffix,
        tone: 'green',
        icon: DataLine
      },
      {
        key: 'nt',
        label: '公告',
        value: '0',
        sub: '条未读 · ' + suffix,
        tone: 'violet',
        icon: DataBoard
      }
    ];
  }
  return [
    {
      key: 'co',
      label: '课程',
      value: '8',
      sub: suffix,
      to: '/course',
      tone: 'blue',
      icon: Reading
    },
    {
      key: 'qu',
      label: '题库题量',
      value: '1,248',
      sub: suffix,
      to: '/question',
      tone: 'green',
      icon: Collection
    },
    {
      key: 'pa',
      label: '试卷',
      value: '36',
      sub: suffix,
      to: '/paper',
      tone: 'violet',
      icon: Files
    },
    {
      key: 'ex',
      label: '进行中考试',
      value: '3',
      sub: suffix,
      to: '/exam/teacher',
      tone: 'amber',
      icon: Timer
    }
  ];
});
</script>

<style scoped>
.dash {
  max-width: 1220px;
  margin: 0 auto;
}

/* Welcome / hero */
.row-welcome {
  margin-bottom: 4px;
}

.hero-wrap {
  position: relative;
  display: flex;
  min-height: 280px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 45%, #faf5ff 100%);
  border: 1px solid rgba(64, 158, 255, 0.12);
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.06);
}

.hero-main {
  position: relative;
  z-index: 1;
  flex: 1;
  padding: 28px 28px 32px;
  min-width: 0;
}

.hero-kicker {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #409eff;
  margin-bottom: 8px;
}

.hero-title {
  margin: 0 0 12px;
  font-size: 26px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.hero-role {
  margin: 0 0 12px;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;
}

.hero-lead {
  margin: 0 0 22px;
  font-size: 14px;
  color: #606266;
  line-height: 1.65;
  max-width: 520px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.btn-ic {
  margin-right: 6px;
  vertical-align: middle;
}

.hero-art {
  display: none;
  width: 220px;
  flex-shrink: 0;
  position: relative;
}

@media (min-width: 900px) {
  .hero-art {
    display: block;
  }
}

.hero-blob {
  position: absolute;
  border-radius: 50%;
  opacity: 0.55;
  filter: blur(0.5px);
}

.hero-blob-a {
  width: 140px;
  height: 140px;
  right: 24px;
  top: 40px;
  background: radial-gradient(circle, #409eff 0%, #79bbff 70%, transparent 100%);
}

.hero-blob-b {
  width: 100px;
  height: 100px;
  right: 100px;
  bottom: 48px;
  background: radial-gradient(circle, #b37feb 0%, #d3adf7 70%, transparent 100%);
  opacity: 0.45;
}

.hero-ring {
  position: absolute;
  right: 48px;
  top: 50%;
  width: 72px;
  height: 72px;
  margin-top: -36px;
  border: 3px solid rgba(64, 158, 255, 0.35);
  border-radius: 50%;
}

/* Calendar */
.calendar-shell {
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);
  height: 100%;
  min-height: 320px;
}

.calendar-shell :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.cal-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.dash-cal {
  --el-calendar-cell-width: 36px;
}

.dash-cal :deep(.el-calendar__header) {
  padding: 10px 12px 12px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.dash-cal :deep(.el-calendar__title) {
  font-size: 15px;
  font-weight: 600;
}

.dash-cal :deep(.el-calendar-table thead th) {
  font-size: 12px;
  color: #909399;
  padding: 6px 0;
}

.dash-cal :deep(.el-calendar-table td) {
  border-color: var(--el-border-color-extra-light);
}

.dash-cal :deep(.el-calendar-day) {
  padding: 4px;
  height: 40px;
}

.cal-cell {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: background 0.15s ease;
}

.cal-cell:hover {
  background: rgba(64, 158, 255, 0.08);
}

.cal-cell.is-today {
  background: linear-gradient(135deg, #409eff, #69b1ff);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.35);
}

.cal-date {
  font-size: 13px;
}

/* Section */
.section-label {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 28px 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.section-spaced {
  margin-top: 32px;
}

/* Stats */
.row-stats {
  margin-bottom: 4px;
}

.stat-card {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 20px 18px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 28px rgba(31, 45, 61, 0.1);
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-blue {
  background: linear-gradient(135deg, #e8f4ff, #d9ecff);
  color: #409eff;
}
.icon-green {
  background: linear-gradient(135deg, #e8f8f0, #d4edda);
  color: #67c23a;
}
.icon-violet {
  background: linear-gradient(135deg, #f4eefa, #ede7ff);
  color: #9254de;
}
.icon-amber {
  background: linear-gradient(135deg, #fff8e6, #ffecc7);
  color: #e6a23c;
}

.stat-text {
  min-width: 0;
}

.stat-val {
  font-size: 24px;
  font-weight: 700;
  color: #1d2129;
  letter-spacing: -0.03em;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-top: 6px;
  font-weight: 500;
}

.stat-sub {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 4px;
  line-height: 1.4;
}

/* Tiles */
.row-tiles {
  margin-bottom: 8px;
}

.tile {
  position: relative;
  padding: 24px 22px 20px;
  border-radius: 16px;
  margin-bottom: 20px;
  min-height: 168px;
  cursor: pointer;
  border: 1px solid var(--el-border-color-lighter);
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: transform 0.15s ease, box-shadow 0.2s ease;
  overflow: hidden;
}

.tile::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  opacity: 0.9;
}

.tile-a::before {
  background: linear-gradient(90deg, #409eff, #69b1ff);
}
.tile-b::before {
  background: linear-gradient(90deg, #9254de, #b37feb);
}
.tile-c::before {
  background: linear-gradient(90deg, #67c23a, #95d475);
}

.tile:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 36px rgba(31, 45, 61, 0.12);
}

.tile-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  background: #f5f7fa;
  color: #409eff;
}

.tile-b .tile-icon {
  color: #9254de;
}
.tile-c .tile-icon {
  color: #67c23a;
}

.tile h3 {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  color: #1d2129;
}

.tile p {
  margin: 0 0 14px;
  font-size: 13px;
  color: #86909c;
  line-height: 1.6;
}

.tile-link {
  font-size: 13px;
  font-weight: 500;
  color: #409eff;
}

.tile-b .tile-link {
  color: #9254de;
}
.tile-c .tile-link {
  color: #67c23a;
}
</style>
