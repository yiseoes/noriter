import client from './client';

/** API-GAM-001: 미리보기 URL */
export const getPreviewUrl = (projectId: string) =>
  `/api/projects/${projectId}/game/preview`;

/** API-GAM-002: 게임 파일 목록 */
export const getGameFiles = (projectId: string) =>
  client.get<{ name: string; size: number }[]>(`/projects/${projectId}/game/files`);

/** API-GAM-003: 소스코드 조회 */
export const getGameSource = (projectId: string, fileName: string) =>
  client.get<string>(`/projects/${projectId}/game/source/${fileName}`);

/** API-GAM-004: ZIP 다운로드 */
export const getDownloadUrl = (projectId: string) =>
  `/api/projects/${projectId}/game/download`;
