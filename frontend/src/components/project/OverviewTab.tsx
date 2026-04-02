import type { Artifact, TokenUsage } from '../../types';

interface OverviewTabProps {
  artifacts: Artifact[];
  tokenUsages: TokenUsage[];
  totalTokens: number;
  debugCount: number;
}

const agentColors: Record<string, string> = {
  PLANNING: 'text-agent-planning', CTO: 'text-agent-cto', DESIGN: 'text-agent-design',
  FRONTEND: 'text-agent-frontend', BACKEND: 'text-agent-backend', QA: 'text-agent-qa',
};
const agentBgColors: Record<string, string> = {
  PLANNING: 'bg-agent-planning', CTO: 'bg-agent-cto', DESIGN: 'bg-agent-design',
  FRONTEND: 'bg-agent-frontend', BACKEND: 'bg-agent-backend', QA: 'bg-agent-qa',
};

export default function OverviewTab({ artifacts, tokenUsages, totalTokens, debugCount }: OverviewTabProps) {
  return (
    <div className="grid grid-cols-2 gap-4 max-md:grid-cols-1">
      {/* 산출물 */}
      <div className="bg-bg-secondary border border-border-light rounded-xl p-5">
        <h3 className="text-sm font-semibold text-text-secondary mb-3">산출물</h3>
        {artifacts.map(a => (
          <div key={a.id} className="flex items-center gap-2.5 py-2 border-b border-border-light last:border-none">
            <span className="text-xl">📄</span>
            <div>
              <div className="text-sm font-semibold">
                {a.name} <span className="text-text-muted font-normal">v{a.version}</span>
              </div>
              <div className={`text-[11px] ${agentColors[a.agentRole] || 'text-text-muted'}`}>
                {a.agentRole} · {new Date(a.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* 토큰 사용량 */}
      <div className="bg-bg-secondary border border-border-light rounded-xl p-5">
        <h3 className="text-sm font-semibold text-text-secondary mb-3">
          토큰 사용량 <span className="font-normal text-text-muted">— 총 {totalTokens.toLocaleString()}</span>
        </h3>
        {tokenUsages.map(t => (
          <div key={t.agentRole} className="mb-2">
            <div className="flex justify-between text-xs mb-1">
              <span className={agentColors[t.agentRole]}>{t.agentRole}</span>
              <span>{t.tokens.toLocaleString()}</span>
            </div>
            <div className="h-2 bg-bg-tertiary rounded overflow-hidden">
              <div
                className={`h-full rounded ${agentBgColors[t.agentRole] || 'bg-brand'}`}
                style={{ width: `${totalTokens ? (t.tokens / totalTokens) * 100 : 0}%` }}
              />
            </div>
          </div>
        ))}
        <div className="mt-4 pt-3 border-t border-border-light text-sm text-text-muted">
          디버깅: {debugCount}/3
        </div>
      </div>
    </div>
  );
}
