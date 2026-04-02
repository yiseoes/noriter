import client from './client';
import type { Project, PageResponse } from '../types';

/** API-PRJ-001: 프로젝트 생성 */
export const createProject = (data: { name?: string; genre?: string; requirement: string; demo?: boolean }) =>
  client.post<Project>('/projects', data).then(r => r.data);

/** API-PRJ-002: 프로젝트 목록 */
export const getProjects = (status?: string, page = 0, size = 20) =>
  client.get<PageResponse<Project>>('/projects', { params: { status, page, size } }).then(r => r.data);

/** API-PRJ-003: 프로젝트 상세 */
export const getProject = (id: string) =>
  client.get<Project>(`/projects/${id}`).then(r => r.data);

/** API-PRJ-004: 재시도 */
export const retryProject = (id: string) =>
  client.post(`/projects/${id}/retry`).then(r => r.data);

/** API-PRJ-005: 삭제 */
export const deleteProject = (id: string) =>
  client.delete(`/projects/${id}`).then(r => r.data);

/** API-PRJ-006: 수정 요청 (피드백) */
export const sendFeedback = (id: string, feedback: string) =>
  client.post(`/projects/${id}/feedback`, { feedback }).then(r => r.data);

/** API-PRJ-007: 중단 */
export const cancelProject = (id: string) =>
  client.post(`/projects/${id}/cancel`).then(r => r.data);
