import { defineStore } from 'pinia';

const STORAGE_KEY = 'ctx_course_id';

export const useCourseContextStore = defineStore('courseContext', {
  state: () => ({
    courseId: null as number | null
  }),
  actions: {
    setCourseId(id: number | null) {
      this.courseId = id;
      if (id != null) localStorage.setItem(STORAGE_KEY, String(id));
      else localStorage.removeItem(STORAGE_KEY);
    },
    loadFromStorage() {
      const raw = localStorage.getItem(STORAGE_KEY);
      this.courseId = raw ? Number(raw) : null;
    }
  }
});
