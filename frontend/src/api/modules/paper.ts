import http from '../http'

export function fetchPaperPage(params: { courseId: number; page?: number; size?: number }) {
  return http.get('/api/paper', { params })
}

export function getPaperDetail(id: number) {
  return http.get(`/api/paper/${id}`)
}

export function createPaperManual(data: {
  courseId: number
  title: string
  items: { questionId: number; score: number }[]
}) {
  return http.post('/api/paper/manual', data)
}

export function generatePaperAuto(data: Record<string, unknown>) {
  return http.post('/api/paper/auto-generate', data)
}

export function deletePaper(id: number) {
  return http.delete(`/api/paper/${id}`)
}

export function fetchPaperTemplates(courseId: number) {
  return http.get('/api/paper-template', { params: { courseId } })
}

export function savePaperTemplate(data: { courseId: number; name: string; rules: Record<string, unknown> }) {
  return http.post('/api/paper-template', data)
}

export function generatePaperFromTemplate(templateId: number, data: { title: string; randomSeed?: number }) {
  return http.post(`/api/paper/from-template/${templateId}`, data)
}

export function fetchPaperGenerationLogs(params: { courseId: number; page?: number; size?: number }) {
  return http.get('/api/paper/generation-logs', { params })
}
