import axios from 'axios';
import { getGuestId } from '../hooks/useGuestId';

const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// 모든 요청에 게스트 ID 헤더 추가
client.interceptors.request.use((config) => {
  config.headers['X-Guest-Id'] = getGuestId();
  return config;
});

export default client;
