import { useState } from 'react';
import FilterBar from '../common/FilterBar';
import type { LogEntry } from '../../types';

const agentColors: Record<string, string> = {
  PLANNING: 'text-agent-planning', CTO: 'text-agent-cto', DESIGN: 'text-agent-design',
  FRONTEND: 'text-agent-frontend', BACKEND: 'text-agent-backend', QA: 'text-agent-qa', SYSTEM: 'text-text-muted',
};

interface LogTabProps {
  logs: LogEntry[];
}

export default function LogTab({ logs }: LogTabProps) {
  const [filter, setFilter] = useState('전체');
  const filters = ['전체', 'INFO', 'AGENT', 'WARN', 'ERROR'];

  const filtered = filter === '전체' ? logs : logs.filter(l => l.level === filter);

  return (
    <div>
      <FilterBar filters={filters} active={filter} onChange={setFilter} />
      <div className="mt-4 bg-bg-secondary border border-border-light rounded-xl p-4
                      font-mono text-sm max-h-[400px] overflow-y-auto [-webkit-overflow-scrolling:touch]
                      max-md:text-[11px] max-md:p-3 max-md:max-h-[300px]">
        {filtered.map(log => (
          <div key={log.id} className="flex gap-2.5 py-1.5 border-b border-bg-tertiary last:border-none
                                       max-md:flex-wrap max-md:gap-1">
            <span className="text-text-muted whitespace-nowrap max-md:text-[10px]">{log.timestamp}</span>
            <span className={`font-semibold min-w-[50px] ${
              log.level === 'ERROR' ? 'text-danger' :
              log.level === 'WARN' ? 'text-warning' :
              log.level === 'AGENT' ? agentColors[log.agentRole] : 'text-text-muted'
            }`}>{log.level}</span>
            <span className={`min-w-[60px] font-medium max-md:min-w-0 ${agentColors[log.agentRole] || ''}`}>
              {log.agentRole}
            </span>
            <span>{log.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
