import http from '@/api/http';
import type { PageResult } from './course';

export interface UserRow {
  id: number;
  username: string;
  realName?: string;
  role: string;
  status: number;
}

export async function fetchUserPage(params: {
  page?: number;
  size?: number;
  keyword?: string;
  role?: string;
}) {
  const { data } = await http.get<PageResult<UserRow>>('/api/system/user', { params });
  return data;
}

export async function createUser(body: {
  username: string;
  password: string;
  realName?: string;
  role: string;
  status?: number;
}) {
  const { data } = await http.post<UserRow>('/api/system/user', body);
  return data;
}

export async function updateUser(
  id: number,
  body: { realName?: string; role?: string; status?: number; password?: string }
) {
  const { data } = await http.put<UserRow>(`/api/system/user/${id}`, body);
  return data;
}
