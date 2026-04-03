import client from './client';
import type { LogEntry, AgentMessage, AuditLog, PageResponse } from '../types';

/** API-LOG-001: 로그 조회 */
export const getLogs = (projectId: string, level?: string, agent?: string) =>
  client.get<PageResponse<LogEntry>>(`/projects/${projectId}/logs`, { params: { level, agent } }).then(r => r.data);

/** API-LOG-002: 에이전트 대화 조회 */
export const getMessages = (projectId: string) =>
  client.get<PageResponse<AgentMessage>>(`/projects/${projectId}/messages`).then(r => r.data);

/** API-LOG-003: 감사 로그 */
export const getAuditLogs = (eventType?: string, page = 0, size = 50) =>
  client.get<PageResponse<AuditLog>>('/audit-logs', { params: { eventType, page, size } }).then(r => r.data);

/** API-LOG-004: 에러코드 목록 */
export const getErrorCodes = () =>
  client.get('/error-codes').then(r => r.data);
