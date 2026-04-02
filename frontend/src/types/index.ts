/* ===== 프로젝트 ===== */
export type ProjectStatus = 'CREATED' | 'WAITING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REVISION';
export type Genre = 'PUZZLE' | 'ACTION' | 'ARCADE' | 'SHOOTING' | 'STRATEGY' | 'ETC';
export type StageType = 'PLANNING' | 'CTO' | 'DESIGN' | 'FRONTEND' | 'BACKEND' | 'QA' | 'RELEASE';
export type StageStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'SKIPPED' | 'RETRYING';
export type AgentRole = 'CTO' | 'PLANNING' | 'DESIGN' | 'FRONTEND' | 'BACKEND' | 'QA' | 'SYSTEM';
export type LogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR' | 'AGENT' | 'SYSTEM';
export type MessageType = 'HANDOFF' | 'BUG_REPORT' | 'REVIEW_REQUEST' | 'FEEDBACK';

export interface Project {
  id: string;
  name: string;
  requirement: string;
  genre: string;
  status: ProjectStatus;
  progress: number;
  currentStage: string | null;
  createdAt: string;
  completedAt: string | null;
  demo: boolean;
}

export interface Stage {
  id: string;
  type: StageType;
  status: StageStatus;
  order: number;
}

export interface LogEntry {
  id: string;
  timestamp: string;
  level: LogLevel;
  agentRole: AgentRole;
  message: string;
  errorCode?: string;
}

export interface AgentMessage {
  id: string;
  fromAgent: AgentRole;
  toAgent: AgentRole;
  messageType: MessageType;
  content: string;
  artifactName?: string;
  timestamp: string;
}

export interface Artifact {
  id: string;
  name: string;
  type: string;
  version: number;
  agentRole: AgentRole;
  createdAt: string;
}

export interface TokenUsage {
  agentRole: AgentRole;
  tokens: number;
}

export interface AuditLog {
  id: string;
  eventType: string;
  projectName?: string;
  detail: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
