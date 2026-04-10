import { useState, useEffect, useRef } from 'react';
import type { ProjectStatus } from '../../types';

const stageConfig = [
  { key: 'PLANNING', label: '기획', emoji: '📋', color: '#ae3ec9' },
  { key: 'ARCHITECTURE', label: 'CTO', emoji: '👔', color: '#4263eb' },
  { key: 'DESIGN', label: '디자인', emoji: '🎨', color: '#e64980' },
  { key: 'IMPLEMENTATION', label: '구현', emoji: '💻', color: '#0ca678' },
  { key: 'QA', label: 'QA', emoji: '🔍', color: '#f76707' },
  { key: 'RELEASE', label: '출시', emoji: '🎮', color: '#ff6b6b' },
];

interface PipelineProps {
  projectStatus: ProjectStatus;
  currentStage?: string | null;
  progress?: number;
  isDemo?: boolean;
  onDemoComplete?: () => void;
}

export default function Pipeline({ projectStatus, currentStage, progress = 0, isDemo = false, onDemoComplete }: PipelineProps) {
  const [activeIdx, setActiveIdx] = useState(-1);
  const [completedSet, setCompletedSet] = useState<Set<number>>(new Set());
  const [allDone, setAllDone] = useState(false);
  const timersRef = useRef<number[]>([]);

  // 데모: 순차 애니메이션
  useEffect(() => {
    if (!isDemo) return;
    if (projectStatus === 'COMPLETED') {
      setCompletedSet(new Set(stageConfig.map((_, i) => i)));
      setAllDone(true);
      setActiveIdx(-1);
      onDemoComplete?.();
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

  // 실제 모드: currentStage 기반 실시간 반영
  useEffect(() => {
    if (isDemo) return;

    if (projectStatus === 'COMPLETED') {
      setCompletedSet(new Set(stageConfig.map((_, i) => i)));
      setAllDone(true);
      setActiveIdx(-1);
      return;
    }

    if (projectStatus === 'FAILED' || projectStatus === 'CANCELLED') {
      // 현재 스테이지까지 표시, 마지막은 실패로
      const stageIdx = stageConfig.findIndex(s => s.key === currentStage);
      if (stageIdx >= 0) {
        const done = new Set<number>();
        for (let i = 0; i < stageIdx; i++) done.add(i);
        setCompletedSet(done);
        setActiveIdx(stageIdx);
      }
      setAllDone(false);
      return;
    }

    if (projectStatus === 'IN_PROGRESS' || projectStatus === 'REVISION') {
      const stageIdx = stageConfig.findIndex(s => s.key === currentStage);
      if (stageIdx >= 0) {
        const done = new Set<number>();
        for (let i = 0; i < stageIdx; i++) done.add(i);
        setCompletedSet(done);
        setActiveIdx(stageIdx);
      }
      setAllDone(false);
      return;
    }

    // CREATED 등 초기 상태
    setActiveIdx(-1);
    setCompletedSet(new Set());
    setAllDone(false);
  }, [projectStatus, currentStage, isDemo]);

  const targetProgress = allDone ? 100 : progress > 0 ? progress : (activeIdx >= 0 ? ((activeIdx + 0.5) / stageConfig.length) * 100 : 0);

  // 숫자 카운트업 애니메이션
  const [displayProgress, setDisplayProgress] = useState(0);
  const rafRef = useRef<number>(0);

  useEffect(() => {
    const start = displayProgress;
    const end = targetProgress;
    if (start === end) return;

    const duration = 1200; // ms
    const startTime = performance.now();

    function tick(now: number) {
      const elapsed = now - startTime;
      const t = Math.min(elapsed / duration, 1);
      // ease-out cubic
      const eased = 1 - Math.pow(1 - t, 3);
      const current = start + (end - start) * eased;
      setDisplayProgress(Math.round(current));
      if (t < 1) {
        rafRef.current = requestAnimationFrame(tick);
      }
    }

    rafRef.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafRef.current);
  }, [targetProgress]);

  const isFailed = projectStatus === 'FAILED';

  return (
    <div>
      <div className="flex items-center my-6 max-md:my-4">
        {stageConfig.map((stage, i) => {
          const isDone = completedSet.has(i);
          const isActive = i === activeIdx;
          const isFailedStage = isFailed && isActive;
          return (
            <div key={stage.key} className="flex flex-col items-center flex-1 relative">
              <div className={`w-9 h-9 rounded-full flex items-center justify-center text-sm font-semibold
                              border-2 z-[1] transition-all duration-500
                              max-md:w-[30px] max-md:h-[30px] max-md:text-xs
                ${isDone ? 'border-success bg-success-bg text-success' :
                  isFailedStage ? 'border-danger bg-danger-bg text-danger scale-110' :
                  isActive ? 'border-info bg-info-bg text-info scale-110' :
                  'border-border bg-bg-primary text-text-muted'}`}
                style={isActive && !isFailedStage ? { boxShadow: `0 0 12px ${stage.color}30` } : {}}>
                {isDone ? '✓' : isFailedStage ? '✕' : stage.emoji}
              </div>
              <div className={`text-[11px] mt-1.5 font-medium transition-colors duration-300
                              max-md:text-[9px]
                ${isDone ? 'text-success' :
                  isFailedStage ? 'text-danger' :
                  isActive ? 'text-text-primary' : 'text-text-muted'}`}
                style={isActive && !isFailedStage ? { color: stage.color } : {}}>
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
      <div className="h-2 bg-bg-tertiary rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all duration-300 ease-out
          ${allDone ? 'bg-success' : isFailed ? 'bg-danger' : 'bg-brand'}`}
          style={{ width: `${displayProgress}%` }} />
      </div>

      {/* 상태 텍스트 + 퍼센트 */}
      {projectStatus === 'IN_PROGRESS' && activeIdx >= 0 && (
        <div className="flex justify-between items-center mt-2">
          <div className="text-xs text-text-muted">
            {stageConfig[activeIdx]?.emoji} {stageConfig[activeIdx]?.label} 단계 진행 중...
          </div>
          <div className="text-sm font-bold text-brand">{displayProgress}%</div>
        </div>
      )}
      {isFailed && (
        <div className="flex justify-between items-center mt-2">
          <div className="text-xs text-danger">
            {stageConfig[activeIdx]?.label || ''} 단계에서 문제가 발생했어요
          </div>
          <div className="text-sm font-bold text-danger">{displayProgress}%</div>
        </div>
      )}
      {projectStatus === 'COMPLETED' && (
        <div className="flex justify-between items-center mt-2">
          <div className="text-xs text-success">게임 생성 완료!</div>
          <div className="text-sm font-bold text-success">100%</div>
        </div>
      )}
    </div>
  );
}
