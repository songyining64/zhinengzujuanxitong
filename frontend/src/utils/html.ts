/** 富文本题干预览 / 查重等场景取纯文本 */
export function stripHtml(html: string): string {
  if (!html) return '';
  const d = document.createElement('div');
  d.innerHTML = html;
  return (d.textContent || d.innerText || '').replace(/\s+/g, ' ').trim();
}
