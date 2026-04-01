import { onMounted, onUnmounted, type Ref } from 'vue';
import http from '@/api/http';

/**
 * 切屏/失焦上报（与后端 switch_blur_limit 配合）
 */
export function useExamAntiCheat(recordIdRef: Ref<number | null | undefined>) {
  let timer: ReturnType<typeof setTimeout> | null = null;

  const reportBlur = () => {
    const id = recordIdRef.value;
    if (id == null) return;
    http.post(`/api/exam/record/${id}/blur`).catch(() => {});
  };

  const onVisibility = () => {
    if (document.visibilityState !== 'visible') {
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => reportBlur(), 300);
    }
  };

  onMounted(() => {
    document.addEventListener('visibilitychange', onVisibility);
  });

  onUnmounted(() => {
    document.removeEventListener('visibilitychange', onVisibility);
    if (timer) clearTimeout(timer);
  });

  return { reportBlur };
}
