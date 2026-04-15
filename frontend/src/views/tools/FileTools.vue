<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <span>文件与文本</span>
      </template>
      <p class="hint">
        参考 <a href="https://github.com/songyining64/WebStudy" target="_blank" rel="noopener">WebStudy</a> 类项目的资源上传思路：此处对接本系统已登录接口
        <code>/api/file/upload</code>；文本可在浏览器端打包为 .txt 下载（无需后端）。
      </p>

      <el-tabs>
        <el-tab-pane label="图片 / 文件上传">
          <el-upload
            class="uploader"
            drag
            :show-file-list="false"
            :http-request="onUpload"
            :disabled="uploading"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">需登录；单文件最大 20MB（与后端 spring.servlet.multipart 一致）</div>
            </template>
          </el-upload>

          <div v-if="lastUpload" class="result-box">
            <el-alert type="success" :closable="false" show-icon title="上传成功" />
            <p><strong>原文件名：</strong>{{ lastUpload.originalName }}</p>
            <p><strong>大小：</strong>{{ (lastUpload.sizeBytes / 1024).toFixed(1) }} KB</p>
            <p class="url-row">
              <strong>下载地址：</strong>
              <el-input v-model="fullDownloadUrl" readonly type="textarea" :rows="2" />
            </p>
            <el-button type="primary" @click="copyUrl">复制链接</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="文本导出为 .txt">
          <el-input
            v-model="textContent"
            type="textarea"
            :rows="12"
            placeholder="在此粘贴或编写文本，点击按钮将在本机保存为 txt 文件（纯前端，不上传服务器）"
          />
          <div class="text-actions">
            <el-input v-model="textFilename" placeholder="文件名（不含后缀）" style="max-width: 240px" />
            <el-button type="primary" :disabled="!textContent.trim()" @click="downloadText">下载 .txt</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="按文件 ID 下载">
          <p class="hint">
            对应后端 <code>GET /api/file/download/{id}</code>（需登录，走带 Token 的请求）。
          </p>
          <div class="text-actions">
            <el-input-number v-model="downloadById" :min="1" :step="1" placeholder="文件 ID" />
            <el-button type="primary" :loading="downloadingById" :disabled="!downloadById" @click="runDownloadById">
              下载
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import * as fileApi from '@/api/modules/file';

const uploading = ref(false);
const lastUpload = ref<fileApi.FileUploadResult | null>(null);

const fullDownloadUrl = computed(() => {
  if (!lastUpload.value?.downloadUrl) return '';
  const u = lastUpload.value.downloadUrl;
  if (u.startsWith('http')) return u;
  return `${window.location.origin}${u.startsWith('/') ? '' : '/'}${u}`;
});

async function onUpload(opt: { file: File; onSuccess?: (v: unknown) => void; onError?: (e: Error) => void }) {
  uploading.value = true;
  lastUpload.value = null;
  try {
    const file = opt.file as File;
    const data = await fileApi.uploadFile(file);
    lastUpload.value = data;
    ElMessage.success('上传成功');
    opt.onSuccess?.(data as never);
  } catch (e) {
    opt.onError?.(e as Error);
  } finally {
    uploading.value = false;
  }
}

function copyUrl() {
  if (!fullDownloadUrl.value) return;
  navigator.clipboard.writeText(fullDownloadUrl.value).then(
    () => ElMessage.success('已复制到剪贴板'),
    () => ElMessage.warning('复制失败，请手动选择文本复制')
  );
}

const textContent = ref('');
const textFilename = ref('笔记');

const downloadById = ref<number | undefined>();
const downloadingById = ref(false);

async function runDownloadById() {
  const id = downloadById.value;
  if (!id) return;
  downloadingById.value = true;
  try {
    const blob = await fileApi.downloadFileBlob(id);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `file-${id}`;
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('已开始下载');
  } catch {
    /* 拦截器已提示 */
  } finally {
    downloadingById.value = false;
  }
}

function downloadText() {
  const name = (textFilename.value || 'export').replace(/[^\w\u4e00-\u9fa5\-_.]/g, '_') || 'export';
  const blob = new Blob([textContent.value], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${name}.txt`;
  a.click();
  URL.revokeObjectURL(url);
  ElMessage.success('已开始下载');
}
</script>

<style scoped>
.page {
  max-width: 900px;
}
.hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.hint code {
  font-size: 12px;
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 4px;
}
.uploader {
  max-width: 480px;
}
.result-box {
  margin-top: 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.result-box p {
  margin: 8px 0;
  font-size: 14px;
}
.url-row {
  margin-top: 12px;
}
.text-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
</style>
