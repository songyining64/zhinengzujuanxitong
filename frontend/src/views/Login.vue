<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">智能组卷系统</h2>
      <el-form :model="form" @submit.prevent="onSubmit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="btn" @click="onSubmit" :loading="loading">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const loading = ref(false);

const form = reactive({
  username: '',
  password: ''
});

const onSubmit = async () => {
  if (!form.username || !form.password) return;
  loading.value = true;
  try {
    // TODO: 替换为真实后端登录接口
    const { data } = await axios.post('/api/auth/login', form);
    localStorage.setItem('access_token', data.data.token);
    localStorage.setItem('username', data.data.username);
    router.push('/');
  } catch (e) {
    // TODO: 错误提示
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2f54eb, #722ed1);
}

.login-card {
  width: 380px;
}

.title {
  text-align: center;
  margin-bottom: 24px;
}
.btn {
  width: 100%;
}
</style>

