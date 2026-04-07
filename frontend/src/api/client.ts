import axios from 'axios';
import { getGuestId } from '../hooks/useGuestId';

const TOKEN_KEY = 'noriter-token';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
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

// 모든 요청에 게스트 ID + JWT 토큰 헤더 추가
client.interceptors.request.use((config) => {
  config.headers['X-Guest-Id'] = getGuestId();
  const token = getToken();
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

export default client;
