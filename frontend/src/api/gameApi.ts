import client from './client';

/** API-GAM-001: 미리보기 URL */
export const getPreviewUrl = (projectId: string) =>
  `/api/projects/${projectId}/preview`;

/** API-GAM-002: 게임 파일 목록 */
export const getGameFiles = (projectId: string) =>
  client.get<{ name: string; size: number }[]>(`/projects/${projectId}/source`).then(r => r.data);

/** API-GAM-003: 소스코드 조회 */
export const getGameSource = (projectId: string, path: string) =>
  client.get<string>(`/projects/${projectId}/source/${path}`).then(r => r.data);

/** API-GAM-004: ZIP 다운로드 */
export const getDownloadUrl = (projectId: string) =>
  `/api/projects/${projectId}/download`;
