import { useState, useEffect, useRef } from 'react';
import type { ProjectStatus } from '../../types';

const stageConfig = [
  { key: 'PLANNING', label: '기획', emoji: '📋', color: '#ae3ec9' },
  { key: 'CTO', label: 'CTO', emoji: '👔', color: '#4263eb' },
  { key: 'DESIGN', label: '디자인', emoji: '🎨', color: '#e64980' },
  { key: 'IMPLEMENT', label: '구현', emoji: '💻', color: '#0ca678' },
  { key: 'QA', label: 'QA', emoji: '🔍', color: '#f76707' },
  { key: 'RELEASE', label: '출시', emoji: '🎮', color: '#ff6b6b' },
];

interface PipelineProps {
  projectStatus: ProjectStatus;
  isDemo?: boolean;
  onDemoComplete?: () => void;
}

export default function Pipeline({ projectStatus, isDemo = false, onDemoComplete }: PipelineProps) {
  const [activeIdx, setActiveIdx] = useState(-1);
  const [completedSet, setCompletedSet] = useState<Set<number>>(new Set());
  const [allDone, setAllDone] = useState(false);
  const timersRef = useRef<number[]>([]);

  // 데모: 순차 애니메이션
  useEffect(() => {
    if (!isDemo || projectStatus === 'COMPLETED') {
      // 이미 완료된 프로젝트면 전부 완료 표시
      if (projectStatus === 'COMPLETED') {
        setCompletedSet(new Set(stageConfig.map((_, i) => i)));
        setAllDone(true);
        setActiveIdx(-1);
        onDemoComplete?.();
      }
      return;
    }

    let idx = 0;
    function step() {
      if (idx >= stageConfig.length) {
        setAllDone(true);
        setActiveIdx(-1);
        onDemoComplete?.();
        return;
      }
      setActiveIdx(idx);
      const t = window.setTimeout(() => {
        setCompletedSet(prev => new Set([...prev, idx]));
        idx++;
        step();
      }, 1500);
      timersRef.current.push(t);
    }
    step();

    return () => {
      timersRef.current.forEach(t => clearTimeout(t));
      timersRef.current = [];
    };
  }, [projectStatus, isDemo]);

  const progress = allDone ? 100 : activeIdx >= 0 ? ((activeIdx + 0.5) / stageConfig.length) * 100 : 0;

  return (
    <div>
      <div className="flex items-center my-6 max-md:my-4">
        {stageConfig.map((stage, i) => {
          const isDone = completedSet.has(i);
          const isActive = i === activeIdx;
          return (
            <div key={stage.key} className="flex flex-col items-center flex-1 relative">
              <div className={`w-9 h-9 rounded-full flex items-center justify-center text-sm font-semibold
                              border-2 z-[1] transition-all duration-500
                              max-md:w-[30px] max-md:h-[30px] max-md:text-xs
                ${isDone ? 'border-success bg-success-bg text-success' :
                  isActive ? 'border-info bg-info-bg text-info scale-110' :
                  'border-border bg-bg-primary text-text-muted'}`}
                style={isActive ? { boxShadow: `0 0 12px ${stage.color}30` } : {}}>
                {isDone ? '✓' : stage.emoji}
              </div>
              <div className={`text-[11px] mt-1.5 font-medium transition-colors duration-300
                              max-md:text-[9px]
                ${isDone ? 'text-success' : isActive ? 'text-text-primary' : 'text-text-muted'}`}
                style={isActive ? { color: stage.color } : {}}>
                {stage.label}
              </div>
              {i < stageConfig.length - 1 && (
                <div className={`absolute top-[18px] left-1/2 w-full h-0.5 z-0 transition-colors duration-500
                  ${isDone ? 'bg-success' : 'bg-border-light'}`} />
              )}
            </div>
          );
        })}
      </div>

      {/* 프로그레스 바 */}
      <div className="h-1.5 bg-bg-tertiary rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all duration-1000 ease-out
          ${allDone ? 'bg-success' : 'bg-brand'}`}
          style={{ width: `${progress}%` }} />
      </div>
    </div>
  );
}
