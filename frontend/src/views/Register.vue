<template>
  <div class="auth-container">
    <div class="login-bg" aria-hidden="true" />
    <div class="bg-overlay" />

    <div class="form-wrapper">
      <div class="logo-container">
        <div class="logo-large"><span>EX</span></div>
        <h2>创建学生账号</h2>
        <p class="subtitle">注册后使用用户名登录（与 <a href="https://github.com/songyining64/WebStudy" target="_blank" rel="noopener">WebStudy</a> 项目类似的独立注册流程）</p>
      </div>

      <el-form class="auth-form" @submit.prevent="onSubmit">
        <div class="form-group">
          <div class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12a5 5 0 1 0-5-5 5 5 0 0 0 5 5zm6 2a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1 7 7 0 0 0 7 7 7 7 0 0 0 7-7z"/></svg>
          </div>
          <el-input v-model="form.username" size="large" placeholder="用户名（3～64 位）" class="form-input-el" maxlength="64" autocomplete="username" />
        </div>
        <div class="form-group">
          <div class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M3 5h18v2H3V5zm0 6h18v2H3v-2zm0 6h18v2H3v-2z"/></svg>
          </div>
          <el-input v-model="form.realName" size="large" placeholder="真实姓名（选填）" class="form-input-el" maxlength="64" />
        </div>
        <div class="form-group">
          <div class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2z"/></svg>
          </div>
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码（至少 6 位）" class="form-input-el" autocomplete="new-password" />
        </div>
        <div class="form-group">
          <div class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2z"/></svg>
          </div>
          <el-input v-model="form.confirmPassword" size="large" type="password" show-password placeholder="确认密码" class="form-input-el" autocomplete="new-password" />
        </div>

        <label class="terms">
          <el-checkbox v-model="form.agree">我已阅读并同意使用本系统用于课程学习相关用途</el-checkbox>
        </label>

        <button type="submit" class="submit-btn" :disabled="loading || !form.agree">
          <span>{{ loading ? '提交中…' : '创建账户' }}</span>
        </button>
      </el-form>

      <div class="footer-links">
        <p>已有账号？<router-link to="/login">立即登录</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import * as authApi from '@/api/modules/auth';

const router = useRouter();
const loading = ref(false);
const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  agree: false
});

const onSubmit = async () => {
  if (!form.username.trim() || form.username.trim().length < 3) {
    ElMessage.warning('用户名至少 3 个字符');
    return;
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位');
    return;
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致');
    return;
  }
  if (!form.agree) {
    ElMessage.warning('请勾选同意条款');
    return;
  }
  loading.value = true;
  try {
    await authApi.registerStudent({
      username: form.username.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      realName: form.realName.trim() || undefined
    });
    ElMessage.success('注册成功，请登录');
    await router.push({ path: '/login', query: { registered: '1', u: form.username.trim() } });
  } catch {
    /* http */
  } finally {
    loading.value = false;
  }
};
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
  margin-bottom: 24px;
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
  font-size: 22px;
  margin: 0;
  font-weight: 600;
}

.subtitle {
  margin: 10px 0 0;
  font-size: 12px;
  color: #7f8c8d;
  line-height: 1.5;
}

.subtitle a {
  color: var(--primary);
}

.auth-form {
  width: 100%;
}

.form-group {
  position: relative;
  margin-bottom: 18px;
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

.terms {
  margin: 0 0 18px;
  font-size: 13px;
  color: #606266;
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
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.footer-links {
  margin-top: 22px;
  text-align: center;
  font-size: 14px;
  color: var(--text-dark);
}

.footer-links a {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
}

.footer-links a:hover {
  text-decoration: underline;
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
