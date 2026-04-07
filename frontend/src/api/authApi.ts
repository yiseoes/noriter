import client from './client';
import type { AuthResponse } from '../types';

export const signup = (data: { email: string; password: string; name: string }) =>
  client.post<AuthResponse>('/auth/signup', data).then(r => r.data);

export const login = (data: { email: string; password: string }) =>
  client.post<AuthResponse>('/auth/login', data).then(r => r.data);

export const getMe = () =>
  client.get<AuthResponse>('/auth/me').then(r => r.data);
