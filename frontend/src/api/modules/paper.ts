import http from '@/api/http';
import type { PageResult } from './course';

export interface Paper {
  id: number;
  courseId?: number;
  title?: string;
  mode?: string;
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
  /** 后端存为 JSON 字符串，部分场景可能已解析为对象 */
  rulesJson?: string | Record<string, unknown>;
}

export type PaperRow = Paper;
export type PaperDetailVO = PaperDetail;

export interface PaperGenerationLog {
  id?: number;
  paperId?: number;
  mode?: string;
  durationMs?: number;
  createTime?: string;
}

/** 分页列表，参数为对象或 (courseId, page, size) */
export function fetchPaperPage(
  courseIdOrParams: number | { courseId: number; page?: number; size?: number },
  page = 1,
  size = 20
) {
  let courseId: number;
  let p: number;
  let s: number;
  if (typeof courseIdOrParams === 'object') {
    courseId = courseIdOrParams.courseId;
    p = courseIdOrParams.page ?? 1;
    s = courseIdOrParams.size ?? 20;
  } else {
    courseId = courseIdOrParams;
    p = page;
    s = size;
  }
  return http.get<PageResult<Paper>>('/api/paper', {
    params: { courseId, page: p, size: s }
  });
}

export function getPaperDetail(id: number) {
  return http.get<PaperDetail>(`/api/paper/${id}`);
}

export function fetchPaperDetail(id: number) {
  return getPaperDetail(id);
}

export function createPaperManual(data: {
  courseId: number;
  title: string;
  items: { questionId: number; score: number }[];
}) {
  return http.post<Paper>('/api/paper/manual', data);
}

export const createManualPaper = createPaperManual;

export function generatePaperAuto(data: Record<string, unknown>) {
  return http.post<Paper>('/api/paper/auto-generate', data);
}

export const autoGeneratePaper = generatePaperAuto;

export function generateFromTemplate(templateId: number, body: { title: string; randomSeed?: number }) {
  return http.post<Paper>(`/api/paper/from-template/${templateId}`, body);
}

export const generatePaperFromTemplate = generateFromTemplate;

export function deletePaper(id: number) {
  return http.delete(`/api/paper/${id}`);
}

export function fetchPaperTemplates(courseId: number) {
  return http.get<PaperTemplate[]>('/api/paper-template', { params: { courseId } });
}

export async function fetchPaperTemplateById(id: number) {
  const { data } = await http.get<PaperTemplate>(`/api/paper-template/${id}`);
  return data;
}

export function savePaperTemplate(body: {
  courseId: number;
  name: string;
  rules: Record<string, unknown>;
}) {
  return http.post<PaperTemplate>('/api/paper-template', {
    courseId: body.courseId,
    name: body.name,
    rules: body.rules
  });
}

export function fetchGenerationLogs(courseId: number, page = 1, size = 10) {
  return http.get<PageResult<unknown>>('/api/paper/generation-logs', {
    params: { courseId, page, size }
  });
}

export function fetchPaperGenerationLogs(params: {
  courseId: number;
  page?: number;
  size?: number;
}) {
  return fetchGenerationLogs(params.courseId, params.page ?? 1, params.size ?? 20);
}

export function deletePaperTemplate(id: number) {
  return http.delete(`/api/paper-template/${id}`);
}

export function updatePaperTemplate(
  id: number,
  body: { courseId: number; name: string; rules: Record<string, unknown> }
) {
  return http.put<PaperTemplate>(`/api/paper-template/${id}`, {
    courseId: body.courseId,
    name: body.name,
    rules: body.rules
  });
}
