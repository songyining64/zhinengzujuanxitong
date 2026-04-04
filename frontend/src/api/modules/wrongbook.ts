import http from '../http';
import type { WrongBookRow } from '@/types/models';

export function fetchWrongBookPage(courseId: number, page = 1, size = 20) {
  return http.get<{ records: WrongBookRow[]; total: number }>('/api/wrong-book', {
    params: { courseId, page, size }
  });
}
