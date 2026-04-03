import http from '@/api/http';
import type { PageResult } from './course';

export interface Paper {
  id: number;
  courseId: number;
  title: string;
  mode: string;
  totalScore?: number;
}

export interface PaperQuestionLine {
  paperQuestionId?: number;
  questionId: number;
  questionOrder?: number;
  score: number;
  type?: string;
  stem?: string;
}

export interface PaperDetail {
  paper: Paper;
  questions: PaperQuestionLine[];
}

export interface PaperTemplate {
  id: number;
  courseId: number;
  name: string;
  rulesJson?: string;
}

export async function fetchPaperPage(courseId: number, page = 1, size = 20) {
  const { data } = await http.get<PageResult<Paper>>('/api/paper', {
    params: { courseId, page, size }
  });
  return data;
}

export async function fetchPaperDetail(id: number) {
  const { data } = await http.get<PaperDetail>(`/api/paper/${id}`);
  return data;
}

export async function createManualPaper(body: {
  courseId: number;
  title: string;
  items: { questionId: number; score: number }[];
}) {
  const { data } = await http.post<Paper>('/api/paper/manual', body);
  return data;
}

export async function autoGeneratePaper(body: Record<string, unknown>) {
  const { data } = await http.post<Paper>('/api/paper/auto-generate', body);
  return data;
}

export async function generateFromTemplate(templateId: number, body: { title: string; randomSeed?: number }) {
  const { data } = await http.post<Paper>(`/api/paper/from-template/${templateId}`, body);
  return data;
}

export async function deletePaper(id: number) {
  await http.delete(`/api/paper/${id}`);
}

export async function fetchPaperTemplates(courseId: number) {
  const { data } = await http.get<PaperTemplate[]>('/api/paper-template', { params: { courseId } });
  return data;
}

export async function savePaperTemplate(body: {
  courseId: number;
  name: string;
  rules: Record<string, unknown>;
}) {
  const { data } = await http.post<PaperTemplate>('/api/paper-template', {
    courseId: body.courseId,
    name: body.name,
    rules: body.rules
  });
  return data;
}

export async function fetchGenerationLogs(courseId: number, page = 1, size = 10) {
  const { data } = await http.get<PageResult<unknown>>('/api/paper/generation-logs', {
    params: { courseId, page, size }
  });
  return data;
}
