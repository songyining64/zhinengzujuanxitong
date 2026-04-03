import http from '@/api/http';

export async function registerStudent(body: {
  username: string;
  password: string;
  confirmPassword: string;
  realName?: string;
}) {
  await http.post('/api/auth/register', body);
}
