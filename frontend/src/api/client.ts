import axios from 'axios';
import { getGuestId } from '../hooks/useGuestId';

const TOKEN_KEY = 'noriter-token';

/** JWT exp 체크 — 만료됐으면 즉시 제거하고 null 반환 */
export function getToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (payload.exp && payload.exp * 1000 < Date.now()) {
      localStorage.removeItem(TOKEN_KEY);
      return null;
    }
  } catch {
    // 파싱 실패 = 유효하지 않은 토큰
    localStorage.removeItem(TOKEN_KEY);
    return null;
  }
  return token;
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// 요청 인터셉터: 게스트 ID + JWT 토큰 헤더 추가
client.interceptors.request.use((config) => {
  config.headers['X-Guest-Id'] = getGuestId();
  const token = getToken();
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터: 401/403 받으면 만료된 토큰 자동 제거
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      if (error.config?.url?.includes('/auth/me')) {
        removeToken();
      }
    }
    return Promise.reject(error);
  }
);

export default client;
