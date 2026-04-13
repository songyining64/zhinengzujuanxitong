import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

function readPermsFromStorage(): string[] {
  try {
    const raw = localStorage.getItem('permissions');
    if (!raw) return [];
    const p = JSON.parse(raw) as unknown;
    return Array.isArray(p) ? (p as string[]) : [];
  } catch {
    return [];
  }
}

/** 登录态：与 localStorage 同步，供 HTTP 与各页面统一读取 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('access_token') || '');
  const username = ref(localStorage.getItem('username') || '');
  const role = ref(localStorage.getItem('role') || '');
  const realName = ref(localStorage.getItem('realName') || '');
  const permissions = ref<string[]>(readPermsFromStorage());

  const isLoggedIn = computed(() => !!token.value);
  const displayName = computed(() => realName.value || username.value || '');

  function setSession(payload: {
    token: string;
    username: string;
    role: string;
    realName?: string;
    permissions?: string[];
  }) {
    token.value = payload.token;
    username.value = payload.username;
    role.value = payload.role;
    realName.value = payload.realName ?? '';
    permissions.value = payload.permissions ?? [];
    localStorage.setItem('access_token', payload.token);
    localStorage.setItem('username', payload.username);
    localStorage.setItem('role', payload.role);
    localStorage.setItem('realName', realName.value);
    localStorage.setItem('permissions', JSON.stringify(permissions.value));
  }

  function clear() {
    token.value = '';
    username.value = '';
    role.value = '';
    realName.value = '';
    permissions.value = [];
    localStorage.removeItem('access_token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('realName');
    localStorage.removeItem('permissions');
  }

  return {
    token,
    username,
    role,
    realName,
    permissions,
    isLoggedIn,
    displayName,
    setSession,
    clear
  };
});
