import { useState } from 'react';
import Badge from '../common/Badge';
import Pipeline from './Pipeline';
import TabBar from './TabBar';
import OverviewTab from './OverviewTab';
import LogTab from './LogTab';
import ChatTab from './ChatTab';
import PreviewTab from './PreviewTab';
import SourceTab from './SourceTab';
import type { Project } from '../../types';

import { mockLogs, mockMessages, mockArtifacts, mockTokenUsages, getMockSourceFiles } from '../../mock';

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
}

export default function ProjectDetail({ project, isDemo = true, onCreateReal, onLogin }: ProjectDetailProps) {
  const [activeTab, setActiveTab] = useState('overview');
  const [demoComplete, setDemoComplete] = useState(false);

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

      <Pipeline projectStatus={project.status} isDemo={isDemo} onDemoComplete={() => setDemoComplete(true)} />

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
        <OverviewTab artifacts={mockArtifacts} tokenUsages={mockTokenUsages} totalTokens={9500} debugCount={0} />
      )}
      {activeTab === 'log' && <LogTab logs={mockLogs} />}
      {activeTab === 'chat' && <ChatTab messages={mockMessages} />}
      {activeTab === 'preview' && <PreviewTab projectId={project.id} />}
      {activeTab === 'source' && <SourceTab files={getMockSourceFiles(project.name)} />}
    </div>
  );
}
