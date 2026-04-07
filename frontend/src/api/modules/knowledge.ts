import http from '../http';
import type { KnowledgePoint } from '@/types/models';

export function fetchKnowledgeList(courseId: number) {
  return http.get<KnowledgePoint[]>('/api/knowledge-point', { params: { courseId } });
}

export function createKnowledge(data: {
  courseId: number;
  parentId?: number | null;
  name: string;
  sortOrder?: number;
}) {
  return http.post<KnowledgePoint>('/api/knowledge-point', data);
}

export function updateKnowledge(id: number, data: { name?: string; parentId?: number | null; sortOrder?: number }) {
  return http.put<KnowledgePoint>(`/api/knowledge-point/${id}`, data);
}

export function deleteKnowledge(id: number) {
  return http.delete(`/api/knowledge-point/${id}`);
}
