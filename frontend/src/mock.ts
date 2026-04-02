import type { Project, Stage, LogEntry, AgentMessage, Artifact, TokenUsage } from './types';

export const mockProject: Project = {
  id: 'prj_001', name: '뱀파이어 서바이벌', requirement: '뱀파이어 서바이벌 류 미니게임',
  genre: 'ACTION', status: 'IN_PROGRESS', progress: 57,
  currentStage: 'FRONTEND', createdAt: '2026-03-31T14:30:00', completedAt: null, demo: true,
};

export const mockStages: Stage[] = [
  { id: 'stg_1', type: 'PLANNING', status: 'COMPLETED', order: 1 },
  { id: 'stg_2', type: 'CTO', status: 'COMPLETED', order: 2 },
  { id: 'stg_3', type: 'DESIGN', status: 'COMPLETED', order: 3 },
  { id: 'stg_4', type: 'FRONTEND', status: 'IN_PROGRESS', order: 4 },
  { id: 'stg_5', type: 'QA', status: 'PENDING', order: 5 },
  { id: 'stg_6', type: 'RELEASE', status: 'PENDING', order: 6 },
];

export const mockLogs: LogEntry[] = [
  { id: 'log_1', timestamp: '14:30:01', level: 'INFO', agentRole: 'PLANNING', message: '기획팀이 게임 기획서 작성을 시작합니다.' },
  { id: 'log_2', timestamp: '14:31:15', level: 'AGENT', agentRole: 'PLANNING', message: '기획서 작성 완료. CTO에게 전달.' },
  { id: 'log_3', timestamp: '14:33:45', level: 'ERROR', agentRole: 'FRONTEND', message: 'Claude API Rate Limit NT-ERR-A001', errorCode: 'NT-ERR-A001' },
  { id: 'log_4', timestamp: '14:33:48', level: 'INFO', agentRole: 'FRONTEND', message: '재시도 성공. 프론트엔드 코드 생성 중...' },
];

export const mockMessages: AgentMessage[] = [
  { id: 'msg_1', fromAgent: 'PLANNING', toAgent: 'CTO', messageType: 'HANDOFF', content: '게임 기획서 작성을 완료했습니다.', artifactName: 'plan.json (v1)', timestamp: '14:31:15' },
  { id: 'msg_2', fromAgent: 'CTO', toAgent: 'DESIGN', messageType: 'HANDOFF', content: 'Canvas 2D 기반으로 진행합니다.', artifactName: 'architecture.json (v1)', timestamp: '14:32:00' },
  { id: 'msg_3', fromAgent: 'QA', toAgent: 'CTO', messageType: 'BUG_REPORT', content: 'CRITICAL 버그 1건. 몬스터 스폰 위치 오류.', timestamp: '14:55:00' },
];

export const mockArtifacts: Artifact[] = [
  { id: 'art_1', name: 'plan.json', type: 'PLAN', version: 1, agentRole: 'PLANNING', createdAt: '2026-03-31T14:31:00' },
  { id: 'art_2', name: 'architecture.json', type: 'ARCHITECTURE', version: 1, agentRole: 'CTO', createdAt: '2026-03-31T14:32:00' },
  { id: 'art_3', name: 'design.json', type: 'DESIGN', version: 1, agentRole: 'DESIGN', createdAt: '2026-03-31T14:33:00' },
];

export const mockTokenUsages: TokenUsage[] = [
  { agentRole: 'PLANNING', tokens: 3200 },
  { agentRole: 'CTO', tokens: 2800 },
  { agentRole: 'DESIGN', tokens: 3500 },
];

export const getMockSourceFiles = (title: string) => [
  { name: 'index.html', content: `<!DOCTYPE html>\n<html lang="ko">\n<head>\n  <meta charset="UTF-8">\n  <title>${title}</title>\n  <link rel="stylesheet" href="style.css">\n</head>\n<body>\n  <canvas id="gameCanvas" width="800" height="600"></canvas>\n  <script src="game.js"></script>\n</body>\n</html>` },
  { name: 'style.css', content: 'canvas {\n  display: block;\n  margin: 0 auto;\n  background: #1a1a2e;\n}' },
  { name: 'game.js', content: `// ${title} 게임 로직\nconst canvas = document.getElementById("gameCanvas");\nconst ctx = canvas.getContext("2d");\n\n// TODO: 게임 로직 구현` },
];
