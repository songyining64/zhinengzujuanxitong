/** 与后端 TakeQuestionVO 对齐 */
export interface TakeQuestion {
  questionId: number;
  orderNo: number;
  type: string;
  stem: string;
  optionsJson: string | null;
  score: number;
  userAnswer: string | null;
}

export interface ExamRecordSummary {
  recordId: number;
  examId: number;
  examTitle: string;
  courseId: number;
  recordStatus: string;
  totalScore: number | null;
  passScore: number | null;
  passed: boolean | null;
  rank: number | null;
  rankTotal: number | null;
  scorePublished: boolean;
  switchBlurCount: number | null;
  switchBlurLimit: number | null;
  submittedAt: string | null;
  startedAt: string | null;
  deadlineAt: string | null;
}

export interface ExamStudentRow {
  id: number;
  courseId: number;
  courseName?: string;
  title: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  status: string;
}

export interface ExamStartData {
  recordId: number;
  examId: number;
  startedAt: string;
  durationMinutes: number;
  deadlineAt: string;
  shuffleQuestions?: boolean;
  shuffleOptions?: boolean;
  switchBlurLimit?: number | null;
}
