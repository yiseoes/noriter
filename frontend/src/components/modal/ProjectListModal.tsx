import { useState } from 'react';
import ModalOverlay from './ModalOverlay';
import FilterBar from '../common/FilterBar';
import Badge from '../common/Badge';
import type { Project } from '../../types';

interface ProjectListModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (project: Project) => void;
  projects: Project[];
}

const filters = ['전체', '진행중', '완료', '실패'];

export default function ProjectListModal({ isOpen, onClose, onSelect, projects }: ProjectListModalProps) {
  const [filter, setFilter] = useState('전체');

  const filtered = filter === '전체' ? projects : projects.filter(p => {
    if (filter === '진행중') return p.status === 'IN_PROGRESS';
    if (filter === '완료') return p.status === 'COMPLETED';
    if (filter === '실패') return p.status === 'FAILED';
    return true;
  });

  const genreEmoji: Record<string, string> = {
    ACTION: '🧛', PUZZLE: '🧱', SHOOTING: '🚀', ARCADE: '👾', STRATEGY: '♟️', ETC: '🎲',
  };

  return (
    <ModalOverlay isOpen={isOpen} onClose={onClose} size="lg" title="📂 내 프로젝트">
      <FilterBar filters={filters} active={filter} onChange={setFilter} />
      <div className="mt-4 space-y-2.5">
        {filtered.map(p => (
          <div
            key={p.id}
            onClick={() => { onClose(); onSelect(p); }}
            className="flex justify-between items-center p-4 border border-border-light rounded-xl
                       cursor-pointer hover:border-brand hover:bg-brand-bg transition-colors
                       max-md:p-3"
          >
            <div className="flex items-center gap-3.5">
              <span className="text-[28px] max-md:text-[22px]">{genreEmoji[p.genre] || '🎮'}</span>
              <div>
                <div className="text-[15px] font-semibold max-md:text-sm">{p.name}</div>
                <div className="text-xs text-text-muted mt-0.5">{p.genre} · {p.updatedAt}</div>
                <div className="w-20 h-1 bg-bg-tertiary rounded-sm overflow-hidden mt-1.5">
                  <div
                    className={`h-full rounded-sm ${p.status === 'COMPLETED' ? 'bg-success' : p.status === 'FAILED' ? 'bg-danger' : 'bg-brand'}`}
                    style={{ width: `${p.progress}%` }}
                  />
                </div>
              </div>
            </div>
            <Badge status={p.status} />
          </div>
        ))}
      </div>
    </ModalOverlay>
  );
}
