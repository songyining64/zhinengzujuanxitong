/**
 * 考试相关接口（按业务拆分示例，可逐步从页面内联迁移至此）
 */
import http from '@/api/http';
import type { ExamRecordSummary, ExamStudentRow, ExamStartData, TakeQuestion } from '@/types/exam';

export async function fetchStudentExams(page = 1, size = 50, keyword?: string) {
  return http.get<{ records: ExamStudentRow[]; total: number }>('/api/exam/student', {
    params: { page, size, keyword }
  });
}

export async function startExam(examId: number) {
  return http.post<ExamStartData>(`/api/exam/${examId}/start`);
}

export async function fetchRecordQuestions(recordId: number) {
  return http.get<TakeQuestion[]>(`/api/exam/record/${recordId}/questions`);
}

export async function fetchRecordSummary(recordId: number) {
  return http.get<ExamRecordSummary>(`/api/exam/record/${recordId}/summary`);
}

export async function saveAnswers(recordId: number, answers: { questionId: number; userAnswer: string }[]) {
  return http.post(`/api/exam/record/${recordId}/save-answers`, { answers });
}

export async function submitExam(recordId: number, answers: { questionId: number; userAnswer: string }[]) {
  return http.post(`/api/exam/record/${recordId}/submit`, { answers });
}
