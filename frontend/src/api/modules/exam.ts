import http from '@/api/http';
import type { PageResult } from './course';
import type { ExamRecordSummary, ExamStudentRow, ExamStartData, TakeQuestion } from '@/types/exam';

export interface Exam {
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
  switchBlurLimit?: number;
  status: string;
}

export async function fetchStudentExams(page = 1, size = 50, keyword?: string) {
  const { data } = await http.get<PageResult<ExamStudentRow>>('/api/exam/student', {
    params: { page, size, keyword }
  });
  return data;
}

export async function fetchTeacherExams(courseId: number, page = 1, size = 20) {
  const { data } = await http.get<PageResult<Exam>>('/api/exam/teacher', {
    params: { courseId, page, size }
  });
  return data;
}

export async function fetchExam(id: number) {
  const { data } = await http.get<Exam>(`/api/exam/${id}`);
  return data;
}

export async function createExam(body: Record<string, unknown>) {
  const { data } = await http.post<Exam>('/api/exam', body);
  return data;
}

export async function updateExam(id: number, body: Record<string, unknown>) {
  const { data } = await http.put<Exam>(`/api/exam/${id}`, body);
  return data;
}

export async function publishExam(id: number) {
  await http.post(`/api/exam/${id}/publish`);
}

export async function endExam(id: number) {
  await http.post(`/api/exam/${id}/end`);
}

export async function publishScore(id: number) {
  await http.post(`/api/exam/${id}/publish-score`);
}

export async function unpublishScore(id: number) {
  await http.post(`/api/exam/${id}/unpublish-score`);
}

export async function startExam(examId: number) {
  const { data } = await http.post<ExamStartData>(`/api/exam/${examId}/start`);
  return data;
}

export async function fetchRecordQuestions(recordId: number) {
  const { data } = await http.get<TakeQuestion[]>(`/api/exam/record/${recordId}/questions`);
  return data;
}

export async function fetchRecordSummary(recordId: number) {
  const { data } = await http.get<ExamRecordSummary>(`/api/exam/record/${recordId}/summary`);
  return data;
}

export async function saveAnswers(recordId: number, answers: { questionId: number; userAnswer: string }[]) {
  await http.post(`/api/exam/record/${recordId}/save-answers`, { answers });
}

export async function submitExam(recordId: number, answers: { questionId: number; userAnswer: string }[]) {
  await http.post(`/api/exam/record/${recordId}/submit`, { answers });
}
