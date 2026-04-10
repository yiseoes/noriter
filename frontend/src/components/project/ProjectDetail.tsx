import { useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import Badge from '../common/Badge';
import Pipeline from './Pipeline';
import TabBar from './TabBar';
import OverviewTab from './OverviewTab';
import LogTab from './LogTab';
import ChatTab from './ChatTab';
import PreviewTab from './PreviewTab';
import SourceTab from './SourceTab';
import { useSse } from '../../hooks/useSse';
import { useLogs, useMessages } from '../../hooks/useLogs';
import { useGameFiles } from '../../hooks/useGameFiles';
import { useProject, useRetryProject } from '../../hooks/useProjects';
import type { Project, AuthUser, TokenUsage } from '../../types';

const tabs = [
  { key: 'overview', label: '📋 개요' },
  { key: 'log', label: '📜 로그' },
  { key: 'chat', label: '💬 대화' },
  { key: 'preview', label: '🎮 미리보기' },
  { key: 'source', label: '📄 소스코드' },
];

interface ProjectDetailProps {
  project: Project;
  isDemo?: boolean;
  onCreateReal?: () => void;
  onLogin?: () => void;
  user?: AuthUser | null;
}

export default function ProjectDetail({ project, isDemo = false, onCreateReal, onLogin, user }: ProjectDetailProps) {
  const [activeTab, setActiveTab] = useState('overview');
  const [demoComplete, setDemoComplete] = useState(false);
  const [sseStatus, setSseStatus] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const isInProgress = project.status === 'IN_PROGRESS' || project.status === 'REVISION';
  const isFailed = project.status === 'FAILED';
  const retryMutation = useRetryProject();

  // 실제 API 데이터 조회
  const { data: projectDetail } = useProject(project.id);
  const { data: logsData } = useLogs(project.id);
  const { data: messagesData } = useMessages(project.id);
  const { data: gameFiles } = useGameFiles(project.id, project.status === 'COMPLETED' || project.status === 'FAILED');
  const logs = logsData?.content ?? [];
  const messages = messagesData?.content ?? [];

  // 상세 데이터 (산출물, 토큰)
  const artifacts = projectDetail?.artifacts ?? [];
  const tokenUsage = projectDetail?.tokenUsage;
  const totalTokens = tokenUsage?.total ?? 0;
  const tokenUsages: TokenUsage[] = tokenUsage?.byAgent
    ? Object.entries(tokenUsage.byAgent).map(([agentRole, tokens]) => ({ agentRole: agentRole as TokenUsage['agentRole'], tokens }))
    : [];
  const debugCount = projectDetail?.debugAttempts ?? 0;

  // SSE 실시간 연동 — IN_PROGRESS일 때만 활성화
  const handleSseEvent = useCallback((type: string, data: unknown) => {
    const d = data as Record<string, unknown>;

    const invalidateAll = () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['project', project.id] });
      queryClient.invalidateQueries({ queryKey: ['logs', project.id] });
      queryClient.invalidateQueries({ queryKey: ['messages', project.id] });
    };

    if (type === 'stage-update') {
      invalidateAll();
      setSseStatus(`${d.stageName || d.stageType || ''} 진행 중...`);
    }

    if (type === 'complete') {
      invalidateAll();
      setSseStatus('게임 생성 완료!');
    }

    if (type === 'error') {
      invalidateAll();
      setSseStatus(`오류 발생: ${d.message || '알 수 없는 오류'}`);
    }

    if (type === 'cancelled') {
      invalidateAll();
      setSseStatus('파이프라인이 중단되었습니다.');
    }
  }, [project.id, queryClient]);

  useSse({
    projectId: project.id,
    onEvent: handleSseEvent,
    enabled: isInProgress && !isDemo,
  });

  return (
    <div>
      {isDemo && (
        <div className="mb-4 px-4 py-3 rounded-xl bg-coral-bg border border-brand/20 text-sm text-brand flex items-center gap-2">
          <span className="font-semibold px-2 py-0.5 rounded-md bg-brand text-white text-[11px]">DEMO</span>
          데모 체험 모드입니다. 실제 게임은 생성되지 않으며, AI 파이프라인 흐름을 미리 체험할 수 있어요.
        </div>
      )}

      <h2 className="text-[22px] font-bold mb-1 max-md:text-lg">{project.name}</h2>
      <div className="flex gap-2 items-center mb-2">
        <Badge status={project.status} />
        <span className="text-sm text-text-muted">{project.genre} · {project.progress}%</span>
      </div>

      {/* SSE 실시간 상태 메시지 */}
      {sseStatus && isInProgress && (
        <div className="mb-3 px-3 py-2 rounded-lg bg-info-bg text-info text-sm flex items-center gap-2">
          <span className="inline-block w-2 h-2 bg-info rounded-full animate-pulse" />
          {sseStatus}
        </div>
      )}

      <Pipeline
        projectStatus={project.status}
        currentStage={project.currentStage}
        progress={project.progress}
        isDemo={isDemo}
        onDemoComplete={() => setDemoComplete(true)}
      />

      {/* 실패 시 상세 사유 + 이어서 재시도 */}
      {isFailed && !isDemo && (() => {
        const errorLogs = logs.filter(l => l.level === 'ERROR');
        const lastError = errorLogs[0];
        const warnLogs = logs.filter(l => l.level === 'WARN').slice(0, 3);
        return (
          <div className="my-4 rounded-xl bg-danger-bg/30 border border-danger/20 overflow-hidden">
            {/* 헤더 + 재시도 버튼들 */}
            <div className="p-4">
              <div className="mb-3">
                <div className="text-sm font-semibold text-danger">게임 만들기에 실패했어요 😢</div>
                <div className="text-xs text-text-muted mt-0.5">
                  다시 시도할 수 있어요. 이어서 재시도하면 이미 완료된 단계를 건너뛰어 비용을 절약할 수 있어요.
                </div>
              </div>
              <div className="flex gap-2 max-md:flex-col">
                {project.currentStage && (
                  <button
                    onClick={() => retryMutation.mutate({ id: project.id, fromStage: project.currentStage || undefined })}
                    disabled={retryMutation.isPending}
                    className="flex-1 py-2.5 rounded-xl bg-gradient-to-br from-brand to-[#ffa8a8] text-white
                               text-sm font-bold cursor-pointer border-none hover:scale-105 transition-transform
                               disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {retryMutation.isPending ? '재시도 중...' : `🔄 ${project.currentStage} 부터 이어서 재시도`}
                  </button>
                )}
                <button
                  onClick={() => retryMutation.mutate({ id: project.id })}
                  disabled={retryMutation.isPending}
                  className="flex-1 py-2.5 rounded-xl border-2 border-border bg-bg-primary text-text-primary
                             text-sm font-bold cursor-pointer hover:border-brand hover:text-brand transition-colors
                             disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {retryMutation.isPending ? '재시도 중...' : '🔁 처음부터 다시 시도'}
                </button>
              </div>
            </div>

            {/* 상세 실패 사유 */}
            {(lastError || warnLogs.length > 0) && (
              <div className="px-4 pb-4 pt-0">
                <div className="bg-bg-primary rounded-lg p-3 text-xs space-y-2">
                  {lastError && (
                    <div className="flex gap-2">
                      <span className="text-danger font-bold shrink-0">❌ 오류</span>
                      <span className="text-text-primary">{lastError.message}</span>
                    </div>
                  )}
                  {warnLogs.map((w, i) => (
                    <div key={i} className="flex gap-2">
                      <span className="text-warning font-bold shrink-0">⚠️ 경고</span>
                      <span className="text-text-muted">{w.message}</span>
                    </div>
                  ))}
                  <div className="text-text-muted pt-1 border-t border-border-light">
                    로그 탭에서 전체 기록을 확인할 수 있어요.
                  </div>
                </div>
              </div>
            )}
          </div>
        );
      })()}

      {/* 데모 완료 CTA */}
      {isDemo && demoComplete && (
        <div className="my-6 p-6 rounded-2xl bg-gradient-to-r from-brand/5 to-[#ffa8a8]/10 border border-brand/20 text-center"
             style={{ animation: 'bounceUp 0.6s cubic-bezier(0.34,1.56,0.64,1) both' }}>
          <div className="text-lg font-bold mb-1">🎮 데모 체험이 완료되었어요!</div>
          <div className="text-sm text-text-muted mb-4">실제 AI가 만드는 게임을 경험해보세요.</div>
          <div className="flex gap-3 justify-center max-md:flex-col">
            <div className="relative group">
              <button
                onClick={onCreateReal}
                className="px-7 py-3 rounded-[16px] bg-gradient-to-br from-brand to-[#ffa8a8] text-white
                           text-sm font-bold cursor-pointer border-none hover:scale-105 transition-transform">
                🎮 진짜 게임 만들기
              </button>
              <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-1.5 rounded-lg
                              bg-text-primary text-white text-[11px] whitespace-nowrap
                              opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
                게스트 모드로 1회 무료 생성 가능!
              </div>
            </div>
            <button
              onClick={onLogin}
              className="px-7 py-3 rounded-[16px] border-2 border-border-light bg-bg-primary text-text-primary
                         text-sm font-bold cursor-pointer hover:border-brand hover:text-brand transition-colors">
              🔑 로그인하기
            </button>
          </div>
        </div>
      )}

      <TabBar tabs={tabs} active={activeTab} onChange={setActiveTab} />

      {activeTab === 'overview' && (
        <OverviewTab artifacts={artifacts} tokenUsages={tokenUsages} totalTokens={totalTokens} debugCount={debugCount} />
      )}
      {activeTab === 'log' && <LogTab logs={logs} />}
      {activeTab === 'chat' && <ChatTab messages={messages} />}
      {activeTab === 'preview' && (
        <PreviewTab
          projectId={project.id}
          projectStatus={project.status}
          isGuest={!user}
          feedbackCount={0}
          isAdmin={user?.role === 'ADMIN'}
        />
      )}
      {activeTab === 'source' && <SourceTab files={gameFiles ?? []} />}
    </div>
  );
}
