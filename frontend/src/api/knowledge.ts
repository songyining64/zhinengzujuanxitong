import http from './http';

export interface KpRow {
  id: number;
  courseId: number;
  parentId?: number | null;
  name: string;
  sortOrder?: number;
}

export function fetchKnowledgeList(courseId: number) {
  return http.get<KpRow[]>('/api/knowledge-point', { params: { courseId } });
}

export function createKnowledge(data: {
  courseId: number;
  parentId?: number | null;
  name: string;
  sortOrder?: number;
}) {
  return http.post<KpRow>('/api/knowledge-point', data);
}

export function updateKnowledge(id: number, data: { parentId?: number | null; name?: string; sortOrder?: number }) {
  return http.put<KpRow>(`/api/knowledge-point/${id}`, data);
}

export function deleteKnowledge(id: number) {
  return http.delete(`/api/knowledge-point/${id}`);
}
