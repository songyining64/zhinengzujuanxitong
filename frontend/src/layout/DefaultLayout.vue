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
          <el-sub-menu index="/system">
            <template #title>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/user">用户管理</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="header">
          <div class="left">
            <span>{{ pageTitle }}</span>
          </div>
          <div class="right">
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
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppLogo from '@/components/AppLogo.vue';

const route = useRoute();
const router = useRouter();

const activeMenu = computed(() => route.path);
const pageTitle = computed(() => (route.meta.title as string) || '首页');
const username = computed(() => localStorage.getItem('username') || '');

const logout = () => {
  localStorage.removeItem('access_token');
  localStorage.removeItem('username');
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
</style>

