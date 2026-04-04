/** 仅前端演示用令牌：不请求真实 JWT，便于未启动后端时浏览界面 */
export const FRONTEND_DEMO_TOKEN = '__FRONTEND_DEMO_NO_BACKEND__';

export function isDemoSession(): boolean {
  return localStorage.getItem('access_token') === FRONTEND_DEMO_TOKEN;
}
