import http from '../http';
import type { Course } from '@/types/models';

export function fetchCoursePage(page = 1, size = 20, keyword?: string) {
  return http.get<{ records: Course[]; total: number }>('/api/course', {
    params: { page, size, keyword }
  });
}

export function getCourse(id: number) {
  return http.get<Course>(`/api/course/${id}`);
}

export function createCourse(data: { name: string; description?: string; code?: string }) {
  return http.post<Course>('/api/course', data);
}

export function updateCourse(id: number, data: { name?: string; description?: string; code?: string; status?: number }) {
  return http.put<Course>(`/api/course/${id}`, data);
}

export interface CourseStudentVO {
  studentId: number;
  username: string;
  realName?: string;
}

export function fetchCourseStudents(courseId: number) {
  return http.get<CourseStudentVO[]>(`/api/course/${courseId}/students`);
}

export function addCourseStudent(courseId: number, studentId: number) {
  return http.post(`/api/course/${courseId}/students/${studentId}`);
}

export function removeCourseStudent(courseId: number, studentId: number) {
  return http.delete(`/api/course/${courseId}/students/${studentId}`);
}
