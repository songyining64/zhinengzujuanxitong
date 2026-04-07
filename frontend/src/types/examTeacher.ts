export interface ExamCreateRequest {
  courseId: number;
  paperId: number;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  passScore?: number | null;
  scorePublished?: boolean;
  shuffleQuestions?: boolean;
  shuffleOptions?: boolean;
  switchBlurLimit?: number | null;
}

export interface ExamEntity {
  id: number;
  courseId: number;
  paperId: number;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  passScore?: number | null;
  scorePublished?: number;
  shuffleQuestions?: number;
  shuffleOptions?: number;
  switchBlurLimit?: number | null;
  status: string;
  createTime?: string;
}
