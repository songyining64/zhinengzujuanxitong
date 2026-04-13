import axios, { type AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { useUserStore } from '@/store/user';

/** 统一封装：Bearer、业务 code、401 跳转 */
const http = axios.create({
  baseURL: '/',
  timeout: 60000
});

http.interceptors.request.use((config) => {
  /** 与 Pinia 写入同源，避免在 Pinia 初始化前调用 useUserStore */
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (res: AxiosResponse) => {
    const payload = res.data as { code?: number; msg?: string; data?: unknown };
    if (payload && typeof payload.code === 'number') {
      if (payload.code !== 0) {
        ElMessage.error(payload.msg || '操作失败');
        return Promise.reject(new Error(payload.msg || 'error'));
      }
      res.data = payload.data as typeof res.data;
    }
    return res;
  },
  (err) => {
    const status = err.response?.status;
    const body = err.response?.data as { msg?: string; message?: string } | undefined;
    const msg = body?.msg || body?.message;
    if (status === 401) {
      try {
        useUserStore().clear();
      } catch {
        localStorage.removeItem('access_token');
        localStorage.removeItem('username');
        localStorage.removeItem('role');
        localStorage.removeItem('realName');
        localStorage.removeItem('permissions');
      }
      ElMessage.warning('登录已过期，请重新登录');
      router.push('/login');
    } else if (msg) {
      ElMessage.error(msg);
    } else {
      ElMessage.error(err.message || '网络异常');
    }
    return Promise.reject(err);
  }
);

export default http;
