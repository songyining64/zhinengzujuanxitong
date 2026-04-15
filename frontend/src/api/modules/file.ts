import http from '@/api/http';

export interface FileUploadResult {
  id: number;
  originalName: string;
  downloadUrl: string;
  sizeBytes: number;
}

/** 需登录；与后端 GET /api/file/download/{id} 对应（开发环境走 Vite 代理） */
export function fileDownloadPath(id: number) {
  return `/api/file/download/${id}`;
}

/** 携带 Token 下载文件流（用于无法用浏览器直接打开受保护链接时） */
export async function downloadFileBlob(id: number): Promise<Blob> {
  const res = await http.get(`/api/file/download/${id}`, { responseType: 'blob' });
  return res.data as Blob;
}

/** 需登录；上传成功后返回相对路径下载地址 */
export async function uploadFile(file: File) {
  const fd = new FormData();
  fd.append('file', file);
  const { data } = await http.post<FileUploadResult>('/api/file/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return data;
}
