<template>
  <div class="login-page">
    <div class="login-bg" />
    <el-card class="login-card" shadow="always">
      <div class="brand">
        <div class="logo-mark">卷</div>
        <div>
          <h1 class="title">课程智能组卷系统</h1>
          <p class="subtitle">统一认证后进入教学工作台</p>
        </div>
      </div>

      <el-form :model="form" size="large" @submit.prevent="onSubmit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="密码" show-password clearable />
        </el-form-item>
        <el-form-item class="btn-row">
          <el-button type="primary" class="btn-main" :loading="loading" native-type="submit" @click="onSubmit">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="showDemo" class="demo-block">
        <el-divider>本地开发</el-divider>
        <p class="demo-tip">未启动后端（8080）时，正式登录会失败。可先用演示模式查看界面。</p>
        <el-button class="btn-demo" plain type="primary" @click="enterDemo">演示模式进入（不连后端）</el-button>
      </div>

      <p class="foot-hint">生产环境请使用管理员分配的账号；API 前缀 <code>/api</code> 由开发代理至后端。</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import http from '@/api/http';
import { FRONTEND_DEMO_TOKEN } from '@/constants/auth';

const router = useRouter();
const loading = ref(false);

/** 仅开发构建显示演示入口，生产打包不展示 */
const showDemo = computed(() => import.meta.env.DEV);

const form = reactive({
  username: '',
  password: ''
});

interface LoginData {
  token: string;
  username: string;
  realName?: string;
  role: string;
}

const onSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  loading.value = true;
  try {
    const { data } = await http.post<LoginData>('/api/auth/login', form);
    localStorage.setItem('access_token', data.token);
    localStorage.setItem('username', data.username);
    localStorage.setItem('role', data.role);
    ElMessage.success('登录成功');
    router.push('/');
  } catch {
    /* 已由拦截器提示；常见原因：未启动后端或账号错误 */
  } finally {
    loading.value = false;
  }
};

function enterDemo() {
  localStorage.setItem('access_token', FRONTEND_DEMO_TOKEN);
  localStorage.setItem('username', '演示用户');
  localStorage.setItem('role', 'TEACHER');
  ElMessage.success('已进入演示模式（列表数据需后端或为空）');
  router.push('/');
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(125deg, #1d39c4 0%, #531dab 45%, #391085 100%);
  z-index: 0;
}

.login-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 80% 60% at 20% 20%, rgba(255, 255, 255, 0.12), transparent 55%);
  pointer-events: none;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  border-radius: 12px;
  border: none;
}

.brand {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 28px;
}

.logo-mark {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #409eff, #597ef7);
  color: #fff;
  font-weight: 800;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1.3;
}

.subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

.btn-row {
  margin-bottom: 0;
}

.btn-main {
  width: 100%;
}

.demo-block {
  margin-top: 8px;
}

.demo-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin: 0 0 12px;
}

.btn-demo {
  width: 100%;
}

.foot-hint {
  margin: 16px 0 0;
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.5;
}

.foot-hint code {
  font-size: 11px;
  padding: 1px 6px;
  background: #f4f4f5;
  border-radius: 4px;
}
</style>
