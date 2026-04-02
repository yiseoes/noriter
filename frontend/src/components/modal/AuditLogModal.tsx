import { useState } from 'react';
import ModalOverlay from './ModalOverlay';
import FilterBar from '../common/FilterBar';
import type { AuditLog } from '../../types';

const eventColors: Record<string, string> = {
  STAGE_COMPLETED: 'bg-success-bg text-success',
  PROJECT_CREATED: 'bg-brand-bg text-brand',
  SETTING_CHANGED: 'bg-bg-tertiary text-text-secondary',
  PROJECT_FAILED: 'bg-danger-bg text-danger',
  STAGE_STARTED: 'bg-info-bg text-info',
};

interface AuditLogModalProps {
  isOpen: boolean;
  onClose: () => void;
  logs: AuditLog[];
}

const filters = ['전체', '프로젝트', '스테이지', '설정'];

export default function AuditLogModal({ isOpen, onClose, logs }: AuditLogModalProps) {
  const [filter, setFilter] = useState('전체');

  return (
    <ModalOverlay isOpen={isOpen} onClose={onClose} size="lg" title="📋 감사 로그">
      <FilterBar filters={filters} active={filter} onChange={setFilter} />
      <div className="mt-4 overflow-x-auto [-webkit-overflow-scrolling:touch]">
        <table className="w-full border-collapse text-sm max-md:text-[11px]">
          <thead>
            <tr>
              <th className="text-left p-2.5 font-semibold text-text-secondary border-b-2 border-border-light">시각</th>
              <th className="text-left p-2.5 font-semibold text-text-secondary border-b-2 border-border-light">이벤트</th>
              <th className="text-left p-2.5 font-semibold text-text-secondary border-b-2 border-border-light">프로젝트</th>
              <th className="text-left p-2.5 font-semibold text-text-secondary border-b-2 border-border-light">상세</th>
            </tr>
          </thead>
          <tbody>
            {logs.map(log => (
              <tr key={log.id}>
                <td className="p-2.5 border-b border-border-light max-md:p-1.5">{log.timestamp}</td>
                <td className="p-2.5 border-b border-border-light max-md:p-1.5">
                  <span className={`inline-block px-2 py-0.5 rounded-md text-[11px] font-semibold
                    ${eventColors[log.eventType] || 'bg-bg-tertiary text-text-secondary'}`}>
                    {log.eventType}
                  </span>
                </td>
                <td className="p-2.5 border-b border-border-light max-md:p-1.5">{log.projectName || '—'}</td>
                <td className="p-2.5 border-b border-border-light max-md:p-1.5">{log.detail}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </ModalOverlay>
  );
}
