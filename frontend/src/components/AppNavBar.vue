<template>
  <nav class="navbar">
    <div class="navbar-left">
      <div class="logo-box" @click="$router.push('/')">
        <AppLogo />
        <span class="site-name">课程智能组卷</span>
      </div>
    </div>
    <div class="navbar-center">
      <el-button :class="{ 'nav-active': isHome }" link @click="$router.push('/')">首页</el-button>
      <el-button
        v-for="m in flatMenus"
        :key="m.id"
        :class="{ 'nav-active': isMenuActive(m) }"
        link
        @click="goMenu(m)"
      >
        {{ m.name }}
      </el-button>
    </div>
    <div class="navbar-right">
      <el-tag v-if="roleLabel" size="small" effect="plain" class="role-chip">{{ roleLabel }}</el-tag>
      <el-dropdown v-if="username" trigger="click" :hide-on-click="false">
        <span class="el-dropdown-link">
          <el-avatar :size="32" class="avatar">{{ avatarText }}</el-avatar>
          <span class="username">{{ displayName }}</span>
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <div class="user-preview">
              <el-avatar :size="48" class="preview-avatar">{{ avatarText }}</el-avatar>
              <div class="user-details">
                <strong>{{ displayName }}</strong>
                <div class="user-sub">{{ username }}</div>
                <el-tag v-if="roleLabel" size="small" type="info">{{ roleLabel }}</el-tag>
              </div>
            </div>
            <el-dropdown-item divided @click="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import { ArrowDown, SwitchButton } from '@element-plus/icons-vue';
import AppLogo from '@/components/AppLogo.vue';
import { useUserStore } from '@/store/user';
import type { MenuTreeVO } from '@/types/menu';

const props = defineProps<{
  menuTree: MenuTreeVO[];
}>();

const route = useRoute();
const router = useRouter();

function pathOf(m: MenuTreeVO): string {
  const p = m.path;
  if (!p) return `/menu-${m.id}`;
  return p.startsWith('/') ? p : `/${p}`;
}

function flattenMenus(nodes: MenuTreeVO[]): MenuTreeVO[] {
  const out: MenuTreeVO[] = [];
  function walk(list: MenuTreeVO[]) {
    for (const n of list) {
      if (n.path) out.push(n);
      if (n.children?.length) walk(n.children);
    }
  }
  walk(nodes);
  return out.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.id - b.id);
}

const flatMenus = computed(() => flattenMenus(props.menuTree));

const userStore = useUserStore();
const { username, displayName, role } = storeToRefs(userStore);

const avatarText = computed(() => {
  const n = (displayName.value || username.value || '').trim();
  return n ? n.slice(0, 1).toUpperCase() : '?';
});

const roleMap: Record<string, string> = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
};
const roleLabel = computed(() => {
  const r = role.value;
  return r ? roleMap[r] || r : '';
});

const isHome = computed(() => route.path === '/' || route.path === '');

/** 与 WebStudy 顶栏类似：选中最长匹配的菜单路径，避免 /exam 抢 /exam/take 的高亮 */
const activeMenuId = computed(() => {
  const paths = flatMenus.value
    .map((m) => ({ id: m.id, p: pathOf(m) }))
    .filter((x) => x.p && x.p !== '/')
    .sort((a, b) => b.p.length - a.p.length);
  const rp = route.path;
  for (const { id, p } of paths) {
    if (rp === p || rp.startsWith(p + '/')) return id;
  }
  return null;
});

function isMenuActive(m: MenuTreeVO): boolean {
  return activeMenuId.value === m.id;
}

function goMenu(m: MenuTreeVO) {
  router.push(pathOf(m));
}

function logout() {
  userStore.clear();
  router.push('/login');
}
</script>

<style scoped>
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  background: #eaf4fb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 100;
  flex-shrink: 0;
}

.navbar-left {
  display: flex;
  align-items: center;
  min-width: 200px;
}

.logo-box {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 10px;
}

.site-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: 0.02em;
}

.navbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px 16px;
  align-items: center;
  padding: 0 12px;
}

.navbar-center :deep(.el-button.is-link) {
  color: #2c3e50;
  font-size: 15px;
  font-weight: 500;
}

.navbar-center :deep(.el-button.is-link.nav-active),
.navbar-center :deep(.el-button.is-link.nav-active:hover) {
  color: var(--primary);
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 160px;
  justify-content: flex-end;
}

.role-chip {
  border-color: rgba(52, 152, 219, 0.45) !important;
  color: #2980b9 !important;
  background: rgba(255, 255, 255, 0.8) !important;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.el-dropdown-link:hover {
  background-color: rgba(255, 255, 255, 0.9);
}

.username {
  margin: 0 6px;
  font-size: 14px;
  color: #606266;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark)) !important;
  color: #fff !important;
  font-size: 14px;
  font-weight: 600;
}

.user-preview {
  padding: 14px 16px;
  border-bottom: 1px solid #eee;
  background: #f9fafb;
  min-width: 220px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.preview-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark)) !important;
  color: #fff !important;
  font-size: 18px;
  font-weight: 600;
}

.user-details strong {
  font-size: 15px;
  display: block;
  margin-bottom: 4px;
  color: #303133;
}

.user-sub {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  word-break: break-all;
}
</style>
