import http from '../http';
import type { ExamCreateRequest, ExamEntity } from '@/types/examTeacher';

export function fetchTeacherExams(courseId: number, page = 1, size = 20) {
  return http.get<{ records: ExamEntity[]; total: number }>('/api/exam/teacher', {
    params: { courseId, page, size }
  });
}

export function getExam(id: number) {
  return http.get<ExamEntity>(`/api/exam/${id}`);
}

export function createExam(body: ExamCreateRequest) {
  return http.post<ExamEntity>('/api/exam', body);
}

export function updateExam(id: number, body: ExamCreateRequest) {
  return http.put<ExamEntity>(`/api/exam/${id}`, body);
}

export function publishExam(id: number) {
  return http.post(`/api/exam/${id}/publish`);
}

export function endExam(id: number) {
  return http.post(`/api/exam/${id}/end`);
}

export function publishExamScore(id: number) {
  return http.post(`/api/exam/${id}/publish-score`);
}

export function unpublishExamScore(id: number) {
  return http.post(`/api/exam/${id}/unpublish-score`);
}
