import { useState } from 'react';
import ModalOverlay from './ModalOverlay';
import FilterBar from '../common/FilterBar';
import { useAuditLogs } from '../../hooks/useAuditLogs';

const eventColors: Record<string, string> = {
  STAGE_COMPLETED: 'bg-success-bg text-success',
  STAGE_STARTED: 'bg-info-bg text-info',
  PROJECT_CREATED: 'bg-brand-bg text-brand',
  PROJECT_STATUS_CHANGED: 'bg-warning-bg text-warning',
  PROJECT_DELETED: 'bg-danger-bg text-danger',
  SETTING_CHANGED: 'bg-bg-tertiary text-text-secondary',
};

const filterMap: Record<string, string | undefined> = {
  '전체': undefined,
  '프로젝트': 'PROJECT',
  '스테이지': 'STAGE',
  '설정': 'SETTING',
};

const filters = ['전체', '프로젝트', '스테이지', '설정'];

interface AuditLogModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function AuditLogModal({ isOpen, onClose }: AuditLogModalProps) {
  const [filter, setFilter] = useState('전체');
  const { data, isLoading } = useAuditLogs(filterMap[filter]);

  const logs = data?.content ?? [];

  const formatTime = (ts: string) => {
    const d = new Date(ts);
    return d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <ModalOverlay isOpen={isOpen} onClose={onClose} size="lg" title="📋 감사 로그">
      <FilterBar filters={filters} active={filter} onChange={setFilter} />
      <div className="mt-4 overflow-x-auto [-webkit-overflow-scrolling:touch]">
        {isLoading ? (
          <div className="text-center text-text-muted py-8">로딩 중...</div>
        ) : logs.length === 0 ? (
          <div className="text-center text-text-muted py-8">감사 로그가 없습니다</div>
        ) : (
          <table className="w-full border-collapse text-sm max-md:text-[11px]">
            <thead>
              <tr>
                <th className="text-left p-2.5 font-semibold text-text-secondary border-b-2 border-border-light">시각</th>
                <th className="text-left p-2.5 font-semibold text-text-secondary border-b-2 border-border-light">이벤트</th>
                <th className="text-left p-2.5 font-semibold text-text-secondary border-b-2 border-border-light">상세</th>
              </tr>
            </thead>
            <tbody>
              {logs.map(log => (
                <tr key={log.id}>
                  <td className="p-2.5 border-b border-border-light max-md:p-1.5 whitespace-nowrap">{formatTime(log.timestamp)}</td>
                  <td className="p-2.5 border-b border-border-light max-md:p-1.5">
                    <span className={`inline-block px-2 py-0.5 rounded-md text-[11px] font-semibold
                      ${eventColors[log.eventType] || 'bg-bg-tertiary text-text-secondary'}`}>
                      {log.eventType}
                    </span>
                  </td>
                  <td className="p-2.5 border-b border-border-light max-md:p-1.5">{log.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </ModalOverlay>
  );
}
