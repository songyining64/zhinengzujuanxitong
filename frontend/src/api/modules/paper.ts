import http from '../http'

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
