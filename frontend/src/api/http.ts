import axios, { type AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { FRONTEND_DEMO_TOKEN, isDemoSession } from '@/constants/auth';

/** 统一封装：Bearer、业务 code、401 跳转 */
const http = axios.create({
  baseURL: '/',
  timeout: 60000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  /** 演示令牌不发给后端，避免无效 JWT 触发统一 401 踢回登录 */
  if (token && token !== FRONTEND_DEMO_TOKEN) {
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
      if (isDemoSession()) {
        return Promise.reject(err);
      }
      localStorage.removeItem('access_token');
      localStorage.removeItem('username');
      localStorage.removeItem('role');
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
