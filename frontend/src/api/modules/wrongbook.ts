import http from '@/api/http';
import type { PageResult } from './course';

export interface WrongRow {
  id: number;
  courseId: number;
  questionId: number;
  stem?: string;
  type?: string;
  wrongCount: number;
  lastWrongAt?: string;
}

export async function fetchWrongPage(courseId: number, page = 1, size = 20) {
  const { data } = await http.get<PageResult<WrongRow>>('/api/wrong-book', {
    params: { courseId, page, size }
  });
  return data;
}
