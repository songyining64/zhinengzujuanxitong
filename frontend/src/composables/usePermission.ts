import { useUserStore } from '@/store/user';

/** 与后端菜单权限码一致；ADMIN 拥有全部能力 */
export function getPermissions(): string[] {
  const store = useUserStore();
  if (store.permissions.length) {
    return store.permissions;
  }
  try {
    const raw = localStorage.getItem('permissions');
    if (!raw) return [];
    return JSON.parse(raw) as string[];
  } catch {
    return [];
  }
}

export function hasPerm(code: string): boolean {
  const role = useUserStore().role || localStorage.getItem('role');
  if (role === 'ADMIN') return true;
  return getPermissions().includes(code);
}

export function isRole(...roles: string[]): boolean {
  const r = useUserStore().role || localStorage.getItem('role');
  return r != null && roles.includes(r);
}
