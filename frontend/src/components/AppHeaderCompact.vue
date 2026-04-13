<template>
  <header class="top-header">
    <div class="top-header-inner">
      <div class="top-header-placeholder" aria-hidden="true" />
      <div class="top-header-user">
        <el-tag v-if="roleLabel" size="small" effect="plain" class="role-chip">{{ roleLabel }}</el-tag>
        <el-dropdown v-if="username" trigger="click" :hide-on-click="false">
          <span class="el-dropdown-link">
            <el-avatar :size="34" class="avatar">{{ avatarText }}</el-avatar>
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
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowDown, SwitchButton } from '@element-plus/icons-vue';
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/store/user';

const router = useRouter();
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

function logout() {
  userStore.clear();
  router.push('/login');
}
</script>

<style scoped>
.top-header {
  flex-shrink: 0;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid rgba(52, 152, 219, 0.12);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.8);
}

.top-header-inner {
  height: 100%;
  padding: 0 22px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.top-header-placeholder {
  flex: 1;
}

.top-header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-chip {
  border-color: rgba(52, 152, 219, 0.45) !important;
  color: #2980b9 !important;
  background: rgba(52, 152, 219, 0.06) !important;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 10px;
  transition: background-color 0.2s;
}

.el-dropdown-link:hover {
  background-color: rgba(52, 152, 219, 0.08);
}

.username {
  margin: 0 6px;
  font-size: 14px;
  color: #606266;
  max-width: 140px;
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
