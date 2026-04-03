import http from '@/api/http';

export interface SubjectivePending {
  examRecordId: number;
  examId: number;
  examTitle: string;
  studentId: number;
  studentUsername?: string;
  studentRealName?: string;
  questionId: number;
  stem: string;
  userAnswer?: string;
  maxScore: number;
}

export async function fetchPendingSubjective(examId: number) {
  const { data } = await http.get<SubjectivePending[]>('/api/exam/grading/pending-subjective', {
    params: { examId }
  });
  return data ?? [];
}

export async function submitSubjectiveScore(body: {
  examRecordId: number;
  questionId: number;
  score: number;
}) {
  await http.post('/api/exam/grading/subjective', body);
}
