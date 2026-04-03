import http from '@/api/http';

export interface ExamOverview {
  examId: number;
  submittedCount: number;
  totalRecords: number;
  avgScore?: number;
  maxScore?: number;
  minScore?: number;
  passScore?: number;
  scorePublished?: boolean;
  passCount?: number;
}

export async function fetchOverview(examId: number) {
  const { data } = await http.get<ExamOverview>('/api/exam/analytics/overview', {
    params: { examId }
  });
  return data;
}

export async function fetchRankPage(examId: number, page = 1, size = 20) {
  const { data } = await http.get('/api/exam/analytics/rank', {
    params: { examId, page, size }
  });
  return data;
}
