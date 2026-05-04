import type { AgentMessage } from '../../types';

const avatarConfig: Record<string, { emoji: string; bg: string; color: string }> = {
  PLANNING: { emoji: '📋', bg: 'bg-agent-planning-bg', color: 'text-agent-planning' },
  CTO: { emoji: '👔', bg: 'bg-agent-cto-bg', color: 'text-agent-cto' },
  DESIGN: { emoji: '🎨', bg: 'bg-agent-design-bg', color: 'text-agent-design' },
  FRONTEND: { emoji: '💻', bg: 'bg-agent-frontend-bg', color: 'text-agent-frontend' },
  BACKEND: { emoji: '⚙️', bg: 'bg-agent-backend-bg', color: 'text-agent-backend' },
  QA: { emoji: '🔍', bg: 'bg-agent-qa-bg', color: 'text-agent-qa' },
};

const typeColors: Record<string, string> = {
  HANDOFF: 'bg-info-bg text-info',
  BUG_REPORT: 'bg-danger-bg text-danger',
  REVIEW_REQUEST: 'bg-warning-bg text-warning',
  FEEDBACK: 'bg-brand-bg text-brand',
  CHAT: 'bg-bg-tertiary text-text-secondary',
};

interface ChatTabProps {
  messages: AgentMessage[];
}

export default function ChatTab({ messages }: ChatTabProps) {
  return (
    <div className="bg-bg-secondary border border-border-light rounded-xl p-5 max-h-[400px] overflow-y-auto
                    [-webkit-overflow-scrolling:touch] max-md:p-3.5 max-md:max-h-[350px]">
      {messages.map(msg => {
        const from = avatarConfig[msg.fromAgent] || avatarConfig.CTO;
        const to = avatarConfig[msg.toAgent] || avatarConfig.CTO;
        return (
          <div key={msg.id} className="flex gap-3 mb-5 last:mb-0">
            <div className={`w-9 h-9 rounded-[10px] flex items-center justify-center text-base shrink-0
                            max-md:w-[30px] max-md:h-[30px] max-md:text-sm ${from.bg}`}>
              {from.emoji}
            </div>
            <div className="bg-bg-primary border border-border-light rounded-xl px-4 py-3 max-w-[80%]
                           max-md:max-w-[90%] max-md:px-3 max-md:py-2.5">
              <div className="flex items-center gap-2 mb-1">
                <span className={`text-sm font-semibold ${from.color}`}>{msg.fromAgent}</span>
                <span className="text-text-muted">→</span>
                <span className={`text-sm font-semibold ${to.color}`}>{msg.toAgent}</span>
                <span className={`text-[11px] px-2 py-0.5 rounded-lg font-medium ${typeColors[msg.messageType] || ''}`}>
                  {msg.messageType}
                </span>
              </div>
              <div className="text-sm text-text-secondary">{msg.content}</div>
              {msg.artifactName && (
                <div className="text-xs text-brand mt-1.5">📎 {msg.artifactName}</div>
              )}
              <div className="text-[11px] text-text-muted mt-1">{msg.timestamp}</div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
