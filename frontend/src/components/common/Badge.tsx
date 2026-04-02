import type { ProjectStatus } from '../../types';

const statusConfig: Record<ProjectStatus, { label: string; className: string }> = {
  CREATED: { label: '🆕 생성됨', className: 'bg-brand-bg text-brand' },
  WAITING: { label: '⏳ 대기', className: 'bg-warning-bg text-warning' },
  IN_PROGRESS: { label: '🔄 진행중', className: 'bg-info-bg text-info' },
  COMPLETED: { label: '✓ 완료', className: 'bg-success-bg text-success' },
  FAILED: { label: '✕ 실패', className: 'bg-danger-bg text-danger' },
  CANCELLED: { label: '⊘ 중단', className: 'bg-bg-tertiary text-text-muted' },
  REVISION: { label: '✏️ 수정중', className: 'bg-brand-bg text-brand' },
};

interface BadgeProps {
  status: ProjectStatus;
}

export default function Badge({ status }: BadgeProps) {
  const config = statusConfig[status] || { label: status, className: 'bg-bg-tertiary text-text-muted' };
  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-medium ${config.className}`}>
      {config.label}
    </span>
  );
}
