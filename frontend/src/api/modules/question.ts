import http from '@/api/http';
import type { PageResult } from './course';

export interface Question {
  id: number;
  courseId?: number;
  knowledgePointId?: number;
  type?: string;
  stem?: string;
  difficulty?: number;
  reviewStatus?: string;
}

export function getQuestion(id: number) {
  return http.get<Question>(`/api/question/${id}`);
}

/** 分页查询试题（支持 reviewStatus: DRAFT/PENDING/PUBLISHED/REJECTED） */
export function fetchQuestionPage(params: {
  courseId: number;
  knowledgePointId?: number;
  type?: string;
  keyword?: string;
  reviewStatus?: string;
  page?: number;
  size?: number;
}) {
  return http.get<PageResult<Question>>('/api/question', { params });
}

export async function createQuestion(body: Record<string, unknown>) {
  const { data } = await http.post<Question>('/api/question', body);
  return data;
}

export async function updateQuestion(id: number, body: Record<string, unknown>) {
  const { data } = await http.put<Question>(`/api/question/${id}`, body);
  return data;
}

/** 与后端 POST /api/question/save 一致：无 id 为新增，有 id 为更新 */
export async function saveQuestion(body: Record<string, unknown>) {
  const { data } = await http.post<Question>('/api/question/save', body);
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

export async function fetchQuestionVersion(id: number, versionNo: number) {
  const { data } = await http.get<QuestionVersionVO>(`/api/question/${id}/versions/${versionNo}`);
  return data;
}

export interface QuestionDedupHit {
  questionId?: number;
  similarity?: number;
  type?: string;
  knowledgePointId?: number;
  stemPreview?: string;
}

export interface QuestionDedupResult {
  hits?: QuestionDedupHit[];
  comparedTotal?: number;
  truncated?: boolean;
}

export async function dedupCheckQuestion(body: {
  courseId: number;
  stem: string;
  optionsJson?: string;
  excludeQuestionId?: number;
  knowledgePointId?: number;
  threshold?: number;
}) {
  const { data } = await http.post<QuestionDedupResult>('/api/question/dedup-check', body);
  return data;
}

export async function batchUpdateQuestions(body: {
  courseId: number;
  questionIds: number[];
  knowledgePointId?: number;
  difficulty?: number;
}) {
  const { data } = await http.post<number>('/api/question/batch', body);
  return data;
}
