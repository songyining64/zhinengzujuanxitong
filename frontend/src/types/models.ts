export interface Course {
  id: number;
  teacherId?: number;
  name: string;
  description?: string;
  code?: string;
  status?: number;
  createTime?: string;
}

export interface KnowledgePoint {
  id: number;
  courseId: number;
  parentId: number | null;
  name: string;
  /** 教学要点/说明（示例数据与前端展示用；后端可逐步扩展） */
  content?: string;
  sortOrder?: number;
  createTime?: string;
}

export interface Question {
  id: number;
  courseId: number;
  knowledgePointId: number;
  type: string;
  stem: string;
  optionsJson?: string;
  answer?: string;
  analysis?: string;
  scoreDefault?: number;
  difficulty?: number;
  status?: number;
  reviewStatus?: string;
  versionNo?: number;
  createTime?: string;
}

export interface Paper {
  id: number;
  courseId: number;
  title: string;
  mode?: string;
  totalScore?: number;
  randomSeed?: number | null;
  rulesJson?: string;
  createTime?: string;
}

export interface PaperQuestionLine {
  paperQuestionId: number;
  questionId: number;
  questionOrder: number;
  score: number;
  type: string;
  stem: string;
}

export interface PaperDetailVO {
  paper: Paper;
  questions: PaperQuestionLine[];
}

export interface PaperTemplate {
  id: number;
  courseId: number;
  name: string;
  rulesJson: string;
  createTime?: string;
  updateTime?: string;
}

export interface PaperGenerationLog {
  id: number;
  paperId: number;
  courseId: number;
  operatorId?: number;
  mode: string;
  rulesJson?: string;
  durationMs?: number;
  createTime?: string;
}

export interface PaperManualRequest {
  courseId: number;
  title: string;
  items: { questionId: number; score: number }[];
}

export interface PaperAutoGenRequest {
  courseId: number;
  title: string;
  knowledgePointIds?: number[];
  randomPool?: boolean;
  fixedQuestionIds?: number[];
  includeKnowledgeDescendants?: boolean;
  scorePerQuestion?: number;
  randomSeed?: number | null;
  dedup?: boolean;
  countByType?: Record<string, number>;
  difficultyWeights?: Record<string, number>;
}

export interface ExamOverviewDTO {
  examId: number;
  submittedCount: number;
  totalRecords: number;
  avgScore: number | null;
  maxScore: number | null;
  minScore: number | null;
  passScore: number | null;
  scorePublished: boolean | null;
  passCount: number | null;
}

export interface StudentRankDTO {
  rank: number;
  studentId: number;
  username: string;
  realName?: string;
  totalScore: number | null;
  passed: boolean | null;
}

export interface ExamQuestionStatDTO {
  questionId: number;
  type: string;
  attemptCount: number;
  correctCount: number;
  correctRate: number | null;
}

export interface ExamKnowledgeStatDTO {
  knowledgePointId: number;
  knowledgePointName: string;
  questionCount: number;
  avgCorrectRate: number | null;
}

export interface WrongBookRow {
  id: number;
  courseId: number;
  questionId: number;
  stem: string;
  type: string;
  wrongCount: number;
  lastWrongAt?: string;
}

export interface QuestionDedupResultVO {
  hits: {
    questionId: number;
    similarity: number;
    type: string;
    knowledgePointId: number;
    stemPreview: string;
  }[];
  comparedTotal: number;
  truncated: boolean;
}
