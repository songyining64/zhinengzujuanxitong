import http from './http';

export interface CourseRow {
  id: number;
  name: string;
  description?: string;
  code?: string;
  status?: number;
  teacherId?: number;
}

export function fetchCoursePage(params: { page?: number; size?: number; keyword?: string }) {
  return http.get<{ records: CourseRow[]; total: number }>('/api/course', { params });
}

export function getCourse(id: number) {
  return http.get<CourseRow>(`/api/course/${id}`);
}

export function createCourse(data: { name: string; description?: string; code?: string }) {
  return http.post<CourseRow>('/api/course', data);
}

export function updateCourse(id: number, data: { name?: string; description?: string; code?: string; status?: number }) {
  return http.put<CourseRow>(`/api/course/${id}`, data);
}

export function fetchCourseStudents(courseId: number) {
  return http.get<
    { id: number; studentId: number; username?: string; realName?: string; status?: number; joinTime?: string }[]
  >(`/api/course/${courseId}/students`);
}

export function addCourseStudent(courseId: number, studentId: number) {
  return http.post(`/api/course/${courseId}/students/${studentId}`);
}

export function removeCourseStudent(courseId: number, studentId: number) {
  return http.delete(`/api/course/${courseId}/students/${studentId}`);
}

export function fetchStudentCandidates(params: { page?: number; size?: number; keyword?: string }) {
  return http.get<{ records: { id: number; username: string; realName?: string }[]; total: number }>(
    '/api/course/student-candidates',
    { params }
  );
}
