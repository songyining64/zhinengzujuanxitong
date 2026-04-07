import http from '../http';
import type { Paper, PaperAutoGenRequest, PaperDetailVO, PaperGenerationLog, PaperManualRequest, PaperTemplate } from '@/types/models';

export function fetchPapers(courseId: number, page = 1, size = 20) {
  return http.get<{ records: Paper[]; total: number }>('/api/paper', {
    params: { courseId, page, size }
  });
}

export function getPaperDetail(id: number) {
  return http.get<PaperDetailVO>(`/api/paper/${id}`);
}

export function deletePaper(id: number) {
  return http.delete(`/api/paper/${id}`);
}

export function createManualPaper(body: PaperManualRequest) {
  return http.post<Paper>('/api/paper/manual', body);
}

export function createAutoPaper(body: PaperAutoGenRequest) {
  return http.post<Paper>('/api/paper/auto-generate', body);
}

export function fetchPaperTemplates(courseId: number) {
  return http.get<PaperTemplate[]>('/api/paper-template', { params: { courseId } });
}

export function getPaperTemplate(id: number) {
  return http.get<PaperTemplate>(`/api/paper-template/${id}`);
}

export function savePaperTemplate(body: { courseId: number; name: string; rules: PaperAutoGenRequest }) {
  return http.post<PaperTemplate>('/api/paper-template', body);
}

export function updatePaperTemplate(id: number, body: { courseId: number; name: string; rules: PaperAutoGenRequest }) {
  return http.put<PaperTemplate>(`/api/paper-template/${id}`, body);
}

export function deletePaperTemplate(id: number) {
  return http.delete(`/api/paper-template/${id}`);
}

export function generatePaperFromTemplate(templateId: number, data: { title: string; randomSeed?: number | null }) {
  return http.post<Paper>(`/api/paper/from-template/${templateId}`, data);
}

export function fetchPaperGenerationLogs(params: { courseId: number; page?: number; size?: number }) {
  return http.get<{ records: PaperGenerationLog[]; total: number }>('/api/paper/generation-logs', { params });
}
