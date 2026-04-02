import client from './client';
import type { LogEntry, AgentMessage, AuditLog } from '../types';

/** API-LOG-001: 로그 조회 */
export const getLogs = (projectId: string, level?: string, agent?: string) =>
  client.get<LogEntry[]>(`/projects/${projectId}/logs`, { params: { level, agent } });

/** API-LOG-002: 에이전트 대화 조회 */
export const getMessages = (projectId: string) =>
  client.get<AgentMessage[]>(`/projects/${projectId}/messages`);

/** API-LOG-003: 감사 로그 */
export const getAuditLogs = (eventType?: string) =>
  client.get<AuditLog[]>('/audit-logs', { params: eventType ? { eventType } : {} });

/** API-LOG-004: 에러코드 목록 */
export const getErrorCodes = () =>
  client.get('/error-codes');
