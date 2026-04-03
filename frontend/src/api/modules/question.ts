import http from '@/api/http';
import type { PageResult } from './course';

export interface Question {
  id: number;
  courseId: number;
  knowledgePointId: number;
  type: string;
  stem: string;
  optionsJson?: string;
  answer: string;
  analysis?: string;
  scoreDefault?: number;
  difficulty?: number;
  reviewStatus?: string;
}

export async function fetchQuestionPage(params: {
  courseId: number;
  page?: number;
  size?: number;
  knowledgePointId?: number;
  type?: string;
  keyword?: string;
  reviewStatus?: string;
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
