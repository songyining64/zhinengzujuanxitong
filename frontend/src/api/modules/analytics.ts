import http from '../http';
import type { ExamKnowledgeStatDTO, ExamOverviewDTO, ExamQuestionStatDTO, StudentRankDTO } from '@/types/models';

export function fetchExamOverview(examId: number) {
  return http.get<ExamOverviewDTO>('/api/exam/analytics/overview', { params: { examId } });
}

export function fetchExamRank(examId: number, page = 1, size = 20) {
  return http.get<{ records: StudentRankDTO[]; total: number }>('/api/exam/analytics/rank', {
    params: { examId, page, size }
  });
}

export function fetchQuestionStats(examId: number) {
  return http.get<ExamQuestionStatDTO[]>('/api/exam/analytics/question-stats', { params: { examId } });
}

export function fetchKnowledgePointStats(examId: number) {
  return http.get<ExamKnowledgeStatDTO[]>('/api/exam/analytics/knowledge-point-stats', { params: { examId } });
}
