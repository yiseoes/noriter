import client from './client';

/** API-SET-001: API 키 조회 */
export const getApiKeyStatus = () =>
  client.get<{ configured: boolean; maskedKey?: string }>('/settings/api-key');

/** API-SET-002: API 키 저장 */
export const saveApiKey = (apiKey: string) =>
  client.put('/settings/api-key', { apiKey });

/** API-SET-003: API 키 유효성 검증 */
export const validateApiKey = (apiKey: string) =>
  client.post<{ valid: boolean }>('/settings/api-key/validate', { apiKey });

/** API-SYS-001: 헬스체크 */
export const getHealth = () =>
  client.get<{ server: string; database: string; claudeApi: string }>('/health');
