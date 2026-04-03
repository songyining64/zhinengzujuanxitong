<template>
  <div class="auth-container">
    <!-- 参照 WebStudy：全屏背景 + 遮罩；无素材图时用渐变替代港口大图 -->
    <div class="login-bg" aria-hidden="true" />
    <div class="bg-overlay" />

    <div class="form-wrapper">
      <div class="logo-container">
        <div class="logo-large">
          <span>EX</span>
        </div>
        <h2>课程智能组卷系统</h2>
        <p class="subtitle">题库 · 组卷 · 考试 · 成绩</p>
      </div>

      <el-form class="auth-form" @submit.prevent="onSubmit">
        <div class="form-group">
          <div class="input-icon" aria-hidden="true">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
              />
            </svg>
          </div>
          <el-input
            v-model="form.username"
            size="large"
            placeholder="用户名"
            class="form-input-el"
            autocomplete="username"
          />
        </div>
        <div class="form-group">
          <div class="input-icon" aria-hidden="true">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V10a2 2 0 00-2-2zm-6 9a2 2 0 110-4 2 2 0 010 4zM9 8V6a3 3 0 016 0v2H9z"
              />
            </svg>
          </div>
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            placeholder="密码"
            show-password
            class="form-input-el"
            autocomplete="current-password"
          />
        </div>

        <p class="demo-hint">演示：admin / admin123 · teacher / teacher123 · student / student123</p>

        <div class="footer-links">
          <router-link to="/register">没有账号？注册学生账号</router-link>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <span>{{ loading ? '登录中…' : '登 录' }}</span>
        </button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import http from '@/api/http';
import { useUserStore } from '@/store/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const loading = ref(false);

const form = reactive({
  username: '',
  password: ''
});

interface LoginData {
  token: string;
  username: string;
  realName?: string;
  role: string;
  permissions?: string[];
}

const onSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  loading.value = true;
  try {
    const { data } = await http.post<LoginData>('/api/auth/login', form);
    userStore.setSession({
      token: data.token,
      username: data.username,
      role: data.role,
      realName: data.realName,
      permissions: data.permissions
    });
    ElMessage.success('登录成功');
    router.push('/');
  } catch {
    /* http 拦截器 */
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  const u = route.query.u as string | undefined;
  if (u) form.username = u;
  if (route.query.registered === '1') {
    ElMessage.success('注册成功，请登录');
  }
});
</script>

<style scoped>
.login-bg {
  position: fixed;
  inset: 0;
  z-index: -2;
  background: linear-gradient(135deg, #1a5276 0%, #2980b9 35%, #5dade2 70%, #aed6f1 100%);
  background-size: 200% 200%;
  animation: bgShift 18s ease infinite;
}

@keyframes bgShift {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.form-wrapper {
  background: rgba(255, 255, 255, 0.94);
  padding: 40px 40px 36px;
  border-radius: 16px;
  box-shadow: var(--shadow);
  width: 100%;
  max-width: 440px;
  animation: slideUp 0.55s cubic-bezier(0.18, 0.89, 0.32, 1.28);
}

.logo-container {
  text-align: center;
  margin-bottom: 28px;
}

.logo-large {
  width: 72px;
  height: 72px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 800;
  font-size: 22px;
  letter-spacing: 1px;
  box-shadow: 0 8px 24px rgba(52, 152, 219, 0.35);
}

.logo-container h2 {
  color: var(--text-dark);
  font-size: 24px;
  margin: 0;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: #7f8c8d;
}

.auth-form {
  width: 100%;
}

.form-group {
  position: relative;
  margin-bottom: 20px;
}

.input-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  height: 20px;
  width: 20px;
  color: #8c9aaf;
  z-index: 2;
  pointer-events: none;
}

.form-input-el :deep(.el-input__wrapper) {
  padding-left: 48px;
  border-radius: 12px;
  min-height: 52px;
  box-shadow: none;
  border: 2px solid #e1e5eb;
  background-color: #f8f9fc;
  transition: all 0.25s ease;
}

.form-input-el :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.15);
  background-color: #fff;
}

.demo-hint {
  font-size: 12px;
  color: #7f8c8d;
  line-height: 1.5;
  margin: 0 0 12px;
  text-align: center;
}

.footer-links {
  text-align: center;
  margin-bottom: 18px;
  font-size: 14px;
}

.footer-links a {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}

.footer-links a:hover {
  text-decoration: underline;
}

.submit-btn {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: white;
  border: none;
  border-radius: 12px;
  padding: 15px;
  width: 100%;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 15px rgba(52, 152, 219, 0.3);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(52, 152, 219, 0.4);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(28px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 480px) {
  .form-wrapper {
    padding: 28px 20px;
    margin: 12px;
  }
}
</style>
