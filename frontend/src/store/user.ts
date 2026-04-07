import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

/** 轻量用户状态（可替代多处 localStorage 直读） */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('access_token') || '');
  const username = ref(localStorage.getItem('username') || '');
  const role = ref(localStorage.getItem('role') || '');

  const isLoggedIn = computed(() => !!token.value);

  function setSession(t: string, user: string, r: string) {
    token.value = t;
    username.value = user;
    role.value = r;
    localStorage.setItem('access_token', t);
    localStorage.setItem('username', user);
    localStorage.setItem('role', r);
  }

  function clear() {
    token.value = '';
    username.value = '';
    role.value = '';
    localStorage.removeItem('access_token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
  }

  return { token, username, role, isLoggedIn, setSession, clear };
});
