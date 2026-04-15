<template>
  <el-container class="layout-root" direction="vertical">
    <el-header class="top-nav">
      <div class="top-left">
        <el-button class="mob-only" text @click="drawerMenu = true">☰</el-button>
        <div class="brand">
          <AppLogo class="brand-logo" />
          <span class="brand-title">智能组卷系统</span>
        </div>
        <el-breadcrumb separator="/" class="crumb desk-only">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="crumbTitle">{{ crumbTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="top-right">
        <span class="autosave-tip desk-only">答题页支持自动保存草稿</span>
        <CoursePicker class="top-course" :show-refresh="true" />
        <span v-if="role" class="role-tag">{{ role }}</span>
        <el-dropdown trigger="click">
          <span class="user-link">{{ username || '未登录' }} ▾</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="body-row">
      <el-aside :width="asideW" class="aside desk-aside">
        <el-menu
          :default-active="activeMenu"
          class="menu"
          router
          :collapse="collapsed"
          background-color="#001529"
          text-color="rgba(255,255,255,0.75)"
          active-text-color="#409eff"
        >
          <div class="aside-tools">
            <el-button size="small" text class="collapse-trigger" @click="collapsed = !collapsed">
              {{ collapsed ? '››' : '‹‹' }}
            </el-button>
            <el-button size="small" text class="mob-only" @click="drawerMenu = true">菜单</el-button>
          </div>
          <el-menu-item index="/">
            <span>首页</span>
          </el-menu-item>
          <el-sub-menu index="biz-teach">
            <template #title>教学资源</template>
            <el-menu-item index="/course">课程管理</el-menu-item>
            <el-menu-item index="/knowledge">知识点</el-menu-item>
            <el-menu-item index="/question">题库</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="biz-paper">
            <template #title>智能组卷</template>
            <el-menu-item index="/paper">试卷库</el-menu-item>
            <el-menu-item index="/paper/compose/auto">自动组卷</el-menu-item>
            <el-menu-item index="/paper/compose/manual">手工组卷</el-menu-item>
            <el-menu-item index="/paper/template">组卷模板</el-menu-item>
            <el-menu-item index="/paper/logs">组卷审计</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="biz-exam">
            <template #title>考试与统计</template>
            <el-menu-item index="/exam/teacher">考试管理</el-menu-item>
            <el-menu-item index="/exam/analytics">成绩分析</el-menu-item>
            <el-menu-item index="/wrong-book">错题本</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="biz-sys">
            <template #title>系统</template>
            <el-menu-item index="/system/user">用户管理</el-menu-item>
          </el-sub-menu>
          <el-sub-menu v-if="menuTree.length" index="remote-menu">
            <template #title>更多（后端）</template>
            <el-menu-item v-for="m in menuTree" :key="m.id" :index="menuPath(m)">
              <span>{{ m.name }}</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <el-drawer v-model="drawerMenu" title="导航" direction="ltr" size="240px" class="mob-drawer">
      <el-menu :default-active="activeMenu" router @select="drawerMenu = false">
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/course">课程管理</el-menu-item>
        <el-menu-item index="/question">题库</el-menu-item>
        <el-menu-item index="/paper/compose/auto">自动组卷</el-menu-item>
      </el-menu>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppLogo from '@/components/AppLogo.vue';
import CoursePicker from '@/components/CoursePicker.vue';
import http from '@/api/http';
import type { MenuTreeVO } from '@/types/menu';

const route = useRoute();
const router = useRouter();
const menuTree = ref<MenuTreeVO[]>([]);
const collapsed = ref(false);
const drawerMenu = ref(false);

const activeMenu = computed(() => route.path);
const crumbTitle = computed(() => {
  const t = route.meta.title as string | undefined;
  return t && t !== '首页' ? t : '';
});
const username = computed(() => localStorage.getItem('username') || '');
const role = computed(() => localStorage.getItem('role') || '');

const asideW = computed(() => (collapsed.value ? '64px' : '220px'));

function menuPath(m: MenuTreeVO): string {
  if (m.path && m.path.startsWith('/')) return m.path;
  if (m.path) return `/${m.path}`;
  return `/menu-${m.id}`;
}

onMounted(async () => {
  try {
    const { data } = await http.get<MenuTreeVO[]>('/api/system/menu/tree');
    menuTree.value = data || [];
  } catch {
    menuTree.value = [];
  }
});

const logout = () => {
  localStorage.removeItem('access_token');
  localStorage.removeItem('username');
  localStorage.removeItem('role');
  router.push('/login');
};
</script>

<style scoped>
.layout-root {
  min-height: 100vh;
  background: #f0f2f5;
}

.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: auto !important;
  min-height: 56px;
  box-sizing: border-box;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
  gap: 12px;
  z-index: 100;
  flex-wrap: nowrap;
}

.top-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  min-width: 0;
  flex: 1;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-logo {
  width: 28px;
  height: 28px;
}

.brand-title {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.crumb {
  margin-left: 8px;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
  flex-wrap: nowrap;
}

.autosave-tip {
  font-size: 12px;
  line-height: 1.4;
  color: #909399;
  white-space: nowrap;
}

.top-course {
  display: flex;
  align-items: center;
}

.top-course :deep(.course-picker) {
  align-items: center;
}

@media (max-width: 1100px) {
  .autosave-tip.desk-only {
    display: none !important;
  }

  .top-nav {
    flex-wrap: wrap;
    row-gap: 8px;
    padding-top: 8px;
    padding-bottom: 8px;
  }

  .top-right {
    flex-wrap: wrap;
    justify-content: flex-end;
    width: 100%;
  }
}

@media (max-width: 768px) {
  .top-nav {
    align-items: flex-start;
  }
}

.role-tag {
  font-size: 12px;
  color: #606266;
  background: #f4f4f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.user-link {
  cursor: pointer;
  color: #409eff;
  font-size: 14px;
}

.body-row {
  flex: 1;
  min-height: 0;
}

.aside {
  background-color: #001529;
  overflow-x: hidden;
  transition: width 0.2s ease;
}

.aside-tools {
  padding: 8px;
  display: flex;
  justify-content: flex-end;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.collapse-trigger {
  color: rgba(255, 255, 255, 0.65) !important;
}

.menu {
  border-right: none;
  min-height: calc(100vh - 56px);
}

.main {
  padding: 16px;
  overflow: auto;
  background: #f5f7fa;
}

.mob-only {
  display: none;
}

@media (max-width: 768px) {
  .desk-only {
    display: none !important;
  }
  .mob-only {
    display: inline-flex !important;
  }
  .desk-aside {
    display: none;
  }
  .layout-root {
    flex-direction: column;
  }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
