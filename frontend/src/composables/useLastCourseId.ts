const KEY = 'exam_last_course_id';

export function getLastCourseId(): number | null {
  const v = localStorage.getItem(KEY);
  if (!v) return null;
  const n = Number(v);
  return Number.isFinite(n) && n > 0 ? n : null;
}

export function setLastCourseId(id: number | null) {
  if (id == null || !Number.isFinite(id)) {
    localStorage.removeItem(KEY);
  } else {
    localStorage.setItem(KEY, String(id));
  }
}
