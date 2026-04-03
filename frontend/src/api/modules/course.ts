import http from '@/api/http';

export interface Course {
  id: number;
  teacherId: number;
  name: string;
  description?: string;
  code?: string;
  status?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

export async function fetchCoursePage(params: { page?: number; size?: number; keyword?: string }) {
  const { data } = await http.get<PageResult<Course>>('/api/course', { params });
  return data;
}

export async function createCourse(body: {
  name: string;
  description?: string;
  code?: string;
  teacherId?: number;
}) {
  const { data } = await http.post<Course>('/api/course', body);
  return data;
}

export async function updateCourse(id: number, body: Partial<Course>) {
  const { data } = await http.put<Course>(`/api/course/${id}`, body);
  return data;
}

export interface CourseStudentRow {
  id: number;
  studentId: number;
  username?: string;
  realName?: string;
  status: number;
}

export async function listCourseStudents(courseId: number) {
  const { data } = await http.get<CourseStudentRow[]>(`/api/course/${courseId}/students`);
  return data;
}

export async function addCourseStudent(courseId: number, studentId: number) {
  await http.post(`/api/course/${courseId}/students/${studentId}`);
}

export async function removeCourseStudent(courseId: number, studentId: number) {
  await http.delete(`/api/course/${courseId}/students/${studentId}`);
}
