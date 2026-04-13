import http from './http';

export interface ExamRow {
  id: number;
  courseId: number;
  paperId: number;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  passScore?: number;
  scorePublished?: number;
  shuffleQuestions?: number;
  shuffleOptions?: number;
  switchBlurLimit?: number | null;
  status: string;
}

export function fetchTeacherExams(courseId: number, page = 1, size = 20) {
  return http.get<{ records: ExamRow[]; total: number }>('/api/exam/teacher', {
    params: { courseId, page, size }
  });
}

export function getExam(id: number) {
  return http.get<ExamRow>(`/api/exam/${id}`);
}

export function createExam(data: Record<string, unknown>) {
  return http.post<ExamRow>('/api/exam', data);
}

export function updateExam(id: number, data: Record<string, unknown>) {
  return http.put<ExamRow>(`/api/exam/${id}`, data);
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

export function fetchExamOverview(examId: number) {
  return http.get(`/api/exam/analytics/overview`, { params: { examId } });
}

export function fetchExamRank(examId: number, page = 1, size = 20) {
  return http.get('/api/exam/analytics/rank', { params: { examId, page, size } });
}
