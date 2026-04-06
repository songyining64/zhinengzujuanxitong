import http from '../http'

export function fetchPaperTemplates(courseId: number) {
  return http.get<PaperTemplate[]>('/api/paper-template', { params: { courseId } })
}

export function savePaperTemplate(data: { courseId: number; name: string; rules: Record<string, unknown> }) {
  return http.post('/api/paper-template', data)
}

export function updatePaperTemplate(id: number, data: { courseId: number; name: string; rules: Record<string, unknown> }) {
  return http.put(`/api/paper-template/${id}`, data)
}

export function deletePaperTemplate(id: number) {
  return http.delete(`/api/paper-template/${id}`)
}

export function generatePaperFromTemplate(templateId: number, data: { title: string; randomSeed?: number }) {
  return http.post(`/api/paper/from-template/${templateId}`, data)
}

export function fetchPaperGenerationLogs(params: { courseId: number; page?: number; size?: number }) {
  return http.get<PaperGenerationLogPage>('/api/paper/generation-logs', { params })
}

export function autoGeneratePaper(data: {
  courseId: number
  title: string
  knowledgePointIds?: number[]
  randomPool?: boolean
  fixedQuestionIds?: number[]
  includeKnowledgeDescendants?: boolean
  scorePerQuestion?: number
  randomSeed?: number
  dedup?: boolean
  countByType: Record<string, number>
  difficultyWeights?: Record<string, number>
}) {
  return http.post('/api/paper/auto-generate', data)
}

export function fetchPaperPage(params: { courseId: number; page?: number; size?: number }) {
  return http.get<PaperPage>('/api/paper', { params })
}

export function fetchPaperDetail(id: number) {
  return http.get<PaperDetailVO>(`/api/paper/${id}`)
}

export function deletePaper(id: number) {
  return http.delete(`/api/paper/${id}`)
}

export interface PaperTemplate {
  id: number
  courseId: number
  name: string
  rulesJson?: string
  creatorId?: number
  createTime?: string
  updateTime?: string
}

export interface PaperRow {
  id: number
  courseId: number
  title: string
  mode?: string
  totalScore?: number
  randomSeed?: number
  rulesJson?: string
  creatorId?: number
  createTime?: string
  updateTime?: string
}

export interface PaperPage {
  records: PaperRow[]
  total: number
}

export interface PaperGenerationLog {
  id: number
  paperId?: number
  courseId?: number
  operatorId?: number
  mode?: string
  rulesJson?: string
  durationMs?: number
  createTime?: string
}

export interface PaperGenerationLogPage {
  records: PaperGenerationLog[]
  total: number
}

export interface PaperDetailVO {
  paper: PaperRow
  questions: Array<{
    paperQuestionId: number
    questionId: number
    questionOrder: number
    score: number
    type?: string
    stem?: string
  }>
}
