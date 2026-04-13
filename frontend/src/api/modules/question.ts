import http from '@/api/http';
import type { PageResult } from './course';

export function getQuestion(id: number) {
  return http.get(`/api/question/${id}`)
}

export function createQuestion(data: Record<string, unknown>) {
  return http.post('/api/question', data)
}

export function updateQuestion(id: number, data: Record<string, unknown>) {
  return http.put(`/api/question/${id}`, data)
}

export function deleteQuestion(id: number) {
  return http.delete(`/api/question/${id}`)
}

/** 分页查询试题（支持 reviewStatus: DRAFT/PENDING/PUBLISHED/REJECTED） */
export function fetchQuestionPage(params: {
  courseId: number
  knowledgePointId?: number
  type?: string
  keyword?: string
  reviewStatus?: string
  page?: number
  size?: number
}) {
  const { data } = await http.get<PageResult<Question>>('/api/question', { params });
  return data;
}

export async function createQuestion(body: Record<string, unknown>) {
  const { data } = await http.post<Question>('/api/question', body);
  return data;
}

export async function updateQuestion(id: number, body: Record<string, unknown>) {
  const { data } = await http.put<Question>(`/api/question/${id}`, body);
  return data;
}

export async function deleteQuestion(id: number) {
  await http.delete(`/api/question/${id}`);
}

export interface QuestionVersionVO {
  versionNo: number;
  type?: string;
  stem?: string;
  difficulty?: number;
  reviewStatus?: string;
  createTime?: string;
}

export interface QuestionImportResultVO {
  successCount?: number;
  failCount?: number;
  message?: string;
}

export async function exportQuestionsBlob(courseId: number): Promise<Blob> {
  const res = await http.get('/api/question/export', {
    params: { courseId },
    responseType: 'blob'
  });
  return res.data as Blob;
}

export async function downloadQuestionImportTemplateBlob(): Promise<Blob> {
  const res = await http.get('/api/question/import/template', {
    responseType: 'blob'
  });
  return res.data as Blob;
}

export async function importQuestions(courseId: number, file: File) {
  const fd = new FormData();
  fd.append('file', file);
  const { data } = await http.post<QuestionImportResultVO>('/api/question/import', fd, {
    params: { courseId }
  });
  return data;
}

export async function submitQuestionReview(id: number) {
  await http.post(`/api/question/${id}/submit-review`);
}

export async function approveQuestion(id: number) {
  await http.post(`/api/question/${id}/approve`);
}

export async function rejectQuestion(id: number) {
  await http.post(`/api/question/${id}/reject`);
}

export async function listQuestionVersions(id: number) {
  const { data } = await http.get<QuestionVersionVO[]>(`/api/question/${id}/versions`);
  return data;
}
