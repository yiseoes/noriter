import client from './client';

/** API-GAM-001: 미리보기 URL */
export const getPreviewUrl = (projectId: string) =>
  `/api/projects/${projectId}/preview`;

/** API-GAM-002: 게임 파일 목록 */
export const getGameFiles = (projectId: string) =>
  client.get<{ files: { path: string; size: number; type: string }[] }>(`/projects/${projectId}/source`)
    .then(r => (r.data.files ?? []).map(f => ({ name: f.path, size: f.size })));

/** API-GAM-003: 소스코드 조회 */
export const getGameSource = (projectId: string, path: string) =>
  client.get<string>(`/projects/${projectId}/source/${path}`).then(r => r.data);

/** API-GAM-003b: 소스파일 저장 */
export const saveGameSource = (projectId: string, path: string, content: string) =>
  client.put<{ warnings: string[] }>(`/projects/${projectId}/source/${path}`, content, {
    headers: { 'Content-Type': 'text/plain' },
  }).then(r => r.data);

/** API-GAM-004: ZIP 다운로드 */
export const getDownloadUrl = (projectId: string) =>
  `/api/projects/${projectId}/download`;
