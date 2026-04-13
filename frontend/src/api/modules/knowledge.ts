import http from '@/api/http';

export interface KnowledgePoint {
  id: number;
  courseId: number;
  parentId?: number | null;
  name: string;
  sortOrder: number;
}

export async function listKnowledge(courseId: number) {
  const { data } = await http.get<KnowledgePoint[]>('/api/knowledge-point', {
    params: { courseId }
  });
  return data;
}

export async function createKnowledge(body: {
  courseId: number;
  parentId?: number | null;
  name: string;
  sortOrder?: number;
}) {
  const { data } = await http.post<KnowledgePoint>('/api/knowledge-point', body);
  return data;
}

export async function updateKnowledge(
  id: number,
  body: { parentId?: number | null; name?: string; sortOrder?: number }
) {
  const { data } = await http.put<KnowledgePoint>(`/api/knowledge-point/${id}`, body);
  return data;
}

export async function deleteKnowledge(id: number) {
  await http.delete(`/api/knowledge-point/${id}`);
}
