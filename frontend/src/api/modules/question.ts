import http from '../http'

export function getQuestion(id: number) {
  return http.get(`/api/question/${id}`)
}

export function createQuestion(data: Record<string, unknown>) {
  return http.post('/api/question', data)
}

export function updateQuestion(id: number, data: Record<string, unknown>) {
  return http.put(`/api/question/${id}`, data)
}

export function deleteQuestion(id: number) {
  return http.delete(`/api/question/${id}`)
}

/** 分页查询试题（支持 reviewStatus: DRAFT/PENDING/PUBLISHED/REJECTED） */
export function fetchQuestionPage(params: {
  courseId: number
  knowledgePointId?: number
  type?: string
  keyword?: string
  reviewStatus?: string
  page?: number
  size?: number
}) {
  return http.get('/api/question', { params })
}

/** 导出为 xlsx（走原生 fetch，因响应为二进制非 JSON） */
export async function exportQuestionsBlob(courseId: number): Promise<Blob> {
  const token = localStorage.getItem('access_token')
  const res = await fetch(`/api/question/export?courseId=${courseId}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) throw new Error('导出失败')
  return res.blob()
}

/** 下载导入模板 */
export async function downloadQuestionImportTemplateBlob(): Promise<Blob> {
  const token = localStorage.getItem('access_token')
  const res = await fetch('/api/question/import/template', {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) throw new Error('下载模板失败')
  return res.blob()
}

export function batchUpdateQuestions(data: {
  courseId: number
  questionIds: number[]
  knowledgePointId?: number
  difficulty?: number
}) {
  return http.post('/api/question/batch', data)
}

export function importQuestions(courseId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post(`/api/question/import?courseId=${courseId}`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function submitQuestionReview(id: number) {
  return http.post(`/api/question/${id}/submit-review`)
}

export function approveQuestion(id: number) {
  return http.post(`/api/question/${id}/approve`)
}

export function rejectQuestion(id: number) {
  return http.post(`/api/question/${id}/reject`)
}

export function dedupCheck(data: {
  courseId: number
  stem: string
  optionsJson?: string
  excludeQuestionId?: number
  knowledgePointId?: number
  threshold?: number
}) {
  return http.post('/api/question/dedup-check', data)
}

export function listQuestionVersions(questionId: number) {
  return http.get<QuestionVersionVO[]>(`/api/question/${questionId}/versions`)
}

export function getQuestionVersionSnapshot(questionId: number, versionNo: number) {
  return http.get<QuestionVersionVO>(`/api/question/${questionId}/versions/${versionNo}`)
}

export interface QuestionVersionVO {
  id: number
  versionNo: number
  knowledgePointId: number
  type: string
  stem: string
  optionsJson?: string
  answer: string
  analysis?: string
  scoreDefault: number
  difficulty: number
  status: number
  reviewStatus?: string
  editorId?: number
  createTime: string
}
