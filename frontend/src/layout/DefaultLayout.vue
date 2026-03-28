<template>
  <div class="layout">
    <el-container style="height: 100vh">
      <el-aside width="220px" class="aside">
        <div class="logo">
          <AppLogo />
          <span class="title">智能组卷系统</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="menu"
          router
        >
          <el-menu-item index="/">
            <span>首页</span>
          </el-menu-item>
          <el-menu-item
            v-for="m in menuTree"
            :key="m.id"
            :index="menuPath(m)"
          >
            <span>{{ m.name }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="header">
          <div class="left">
            <span>{{ pageTitle }}</span>
          </div>
          <div class="right">
            <span v-if="role" class="role-tag">{{ role }}</span>
            <el-dropdown>
              <span class="el-dropdown-link">
                {{ username || '未登录' }}
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppLogo from '@/components/AppLogo.vue';
import http from '@/api/http';
import type { MenuTreeVO } from '@/types/menu';

const route = useRoute();
const router = useRouter();

const menuTree = ref<MenuTreeVO[]>([]);

const activeMenu = computed(() => route.path);
const pageTitle = computed(() => (route.meta.title as string) || '首页');
const username = computed(() => localStorage.getItem('username') || '');
const role = computed(() => localStorage.getItem('role') || '');

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
.layout {
  height: 100%;
}

.aside {
  background-color: #001529;
  color: #fff;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  box-sizing: border-box;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo .title {
  margin-left: 8px;
  font-size: 16px;
  font-weight: 600;
}

.menu {
  border-right: none;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  box-sizing: border-box;
  border-bottom: 1px solid #f0f0f0;
}

.main {
  padding: 16px;
  background: #f5f7fa;
}

.role-tag {
  margin-right: 12px;
  font-size: 12px;
  color: #606266;
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>

