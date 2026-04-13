import http from '@/api/http';

export interface FileUploadResult {
  id: number;
  originalName: string;
  downloadUrl: string;
  sizeBytes: number;
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
