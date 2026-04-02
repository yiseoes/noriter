import { useState, useEffect, useRef, useCallback } from 'react';
import AgentDetailPanel from './AgentDetailPanel';

const stages = [
  { agent: '기획팀', emoji: '📋', color: '#ae3ec9', bg: '#f8f0fc', borderColor: 'rgba(174,62,201,0.3)', msg: '게임 기획서를 작성하고 있어요...', done: '기획서 완성! CTO에게 전달할게요 ✨', output: '📄 plan.json → 👔',
    detail: '요구사항을 분석하고 게임 기획서(plan.json)를 생성합니다.', input: '사용자 요구사항 텍스트', outputDesc: '게임 컨셉, 규칙, 목표를 담은 기획서', example: '"뱀파이어 서바이벌 류 게임" → 장르 분석 → 핵심 메커니즘 설계' },
  { agent: 'CTO', emoji: '👔', color: '#4263eb', bg: '#edf2ff', borderColor: 'rgba(66,99,235,0.3)', msg: '아키텍처를 설계하고 있어요...', done: 'Canvas 2D 기반으로 결정! 디자인팀 부탁해요', output: '📐 architecture.json → 🎨',
    detail: '기술 스택과 아키텍처를 결정합니다.', input: '기획서 (plan.json)', outputDesc: '렌더링 방식, 라이브러리, 파일 구조', example: 'Canvas 2D vs WebGL 비교 → Canvas 2D 선택 → 모듈 구조 설계' },
  { agent: '디자인팀', emoji: '🎨', color: '#e64980', bg: '#fff0f6', borderColor: 'rgba(230,73,128,0.3)', msg: 'UI/UX 디자인을 그리고 있어요...', done: '디자인 시안 완료! 개발팀 화이팅 💪', output: '🎨 design.json → 💻⚙️',
    detail: '게임 UI/UX 디자인 스펙을 생성합니다.', input: '기획서 + 아키텍처', outputDesc: '색상 팔레트, 레이아웃, 스프라이트 스펙', example: '다크 테마 + 네온 포인트 → UI 요소별 크기/색상 정의' },
  { agent: '프론트팀', emoji: '💻', color: '#0ca678', bg: '#e6fcf5', borderColor: 'rgba(12,166,120,0.3)', msg: '화면을 만들고 있어요...', done: '렌더링 코드 완성! 백엔드팀과 합칠게요', output: '🖥️ render.js → 🔗 merge',
    detail: '화면 렌더링과 인터랙션 코드를 작성합니다.', input: '디자인 스펙 + 아키텍처', outputDesc: 'HTML/CSS/Canvas 렌더링 코드', example: 'Canvas에 캐릭터/몬스터 그리기 + 애니메이션 루프' },
  { agent: '백엔드팀', emoji: '⚙️', color: '#e67700', bg: '#fff4e6', borderColor: 'rgba(230,119,0,0.3)', msg: '게임 로직을 짜고 있어요...', done: '로직 완성! 코드 합치기 완료 🎮', output: '⚙️ game.js → 🔍 QA',
    detail: '게임 핵심 로직과 상태 관리를 구현합니다.', input: '기획서 + 아키텍처', outputDesc: '게임 로직, 충돌 감지, 점수 시스템', example: '몬스터 스폰 로직 + 자동 공격 시스템 + 레벨업 구현' },
  { agent: 'QA팀', emoji: '🔍', color: '#f76707', bg: '#fff4e6', borderColor: 'rgba(247,103,7,0.3)', msg: '게임을 테스트하고 있어요...', done: '테스트 통과! 게임 완성 🎉', output: '✅ PASS',
    detail: '생성된 코드를 분석하고 버그를 찾습니다.', input: '합쳐진 게임 코드', outputDesc: '테스트 리포트, 버그 목록, PASS/FAIL', example: '구문 오류 검사 → 로직 검증 → 렌더링 테스트 → PASS' },
];

interface PipelinePreviewProps {
  onComplete: () => void;
  onDemo: () => void;
}

export default function PipelinePreview({ onComplete, onDemo }: PipelinePreviewProps) {
  const [activeIdx, setActiveIdx] = useState(-1);
  const [phase, setPhase] = useState<'idle' | 'working' | 'done'>('idle');
  const [completed, setCompleted] = useState<boolean[]>(new Array(6).fill(false));
  const [outputs, setOutputs] = useState<string[]>(new Array(6).fill(''));
  const [allDone, setAllDone] = useState(false);
  const [greenLine, setGreenLine] = useState(false);
  const sectionRef = useRef<HTMLDivElement>(null);
  const progressRef = useRef<HTMLDivElement>(null);
  const startedRef = useRef(false);
  const timersRef = useRef<number[]>([]);
  const abortRef = useRef(false);
  const rafRef = useRef(0);

  const cleanup = useCallback(() => {
    abortRef.current = true;
    timersRef.current.forEach(t => clearTimeout(t));
    timersRef.current = [];
    cancelAnimationFrame(rafRef.current);
    startedRef.current = false;
  }, []);

  const reset = useCallback(() => {
    cleanup();
    abortRef.current = false;
    setActiveIdx(-1);
    setPhase('idle');
    setCompleted(new Array(6).fill(false));
    setOutputs(new Array(6).fill(''));
    setAllDone(false);
    setGreenLine(false);
    if (progressRef.current) {
      progressRef.current.style.transition = 'none';
      progressRef.current.style.transform = 'scaleX(0)';
    }
  }, [cleanup]);

  const run = useCallback(() => {
    if (startedRef.current) return;
    startedRef.current = true;
    reset();
    abortRef.current = false;

    let idx = 0;
    function activate() {
      if (abortRef.current) return;
      if (idx >= stages.length) {
        setGreenLine(true);
        setAllDone(true);
        startedRef.current = false;
        return;
      }
      if (progressRef.current) {
        progressRef.current.style.transition = 'transform 2s linear';
        progressRef.current.style.transform = `scaleX(${(2 * idx + 1) / (2 * stages.length)})`;
      }

      setActiveIdx(idx);
      setPhase('working');

      const t1 = window.setTimeout(() => {
        if (abortRef.current || idx >= stages.length) return;
        const i = idx;
        setPhase('done');
        setCompleted(prev => { const n = [...prev]; n[i] = true; return n; });
        const t2 = window.setTimeout(() => {
          if (abortRef.current || idx >= stages.length) return;
          const i = idx;
          setOutputs(prev => { const n = [...prev]; n[i] = stages[i].output; return n; });
          idx++;
          activate();
        }, 1200);
        timersRef.current.push(t2);
      }, 2000);
      timersRef.current.push(t1);
    }
    rafRef.current = requestAnimationFrame(() => { activate(); });
  }, [reset]);

  useEffect(() => {
    const el = sectionRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && !startedRef.current) run();
    }, { threshold: 0.4 });
    observer.observe(el);
    return () => {
      observer.disconnect();
      cleanup();
    };
  }, [run, cleanup]);

  return (
    <section ref={sectionRef}
      className="min-h-screen flex flex-col items-center justify-center gap-[60px]
                 pt-[30px] px-8 text-center relative snap-start max-md:px-4 max-md:gap-10 max-md:pt-4">
      {/* 상단: 타이틀 + 타임라인 */}
      <div className="w-full max-w-[1100px]">
        <div className="text-sm font-semibold text-brand tracking-widest uppercase mb-2">Meet the Crew & Live Pipeline</div>
        <h2 className="text-[28px] font-bold mb-2 max-md:text-xl">AI 친구들이 이렇게 게임을 만들어요 🚀</h2>
        <p className="text-text-muted text-[15px] mb-10 max-md:mb-6">6명의 에이전트가 순서대로 협업하는 과정을 지켜보세요</p>

        {/* 타임라인 */}
        <div className="relative">
          {/* 배경 라인 */}
          <div className="absolute top-7 h-[3px] bg-border-light rounded-sm z-0 max-md:hidden"
               style={{ left: 'calc(100%/12 + 28px)', right: 0 }} />
          {/* 프로그레스 라인 */}
          <div ref={progressRef}
               className="absolute top-7 h-[3px] rounded-sm z-[1] origin-left max-md:hidden"
               style={{
                 left: 0, right: 0,
                 background: 'linear-gradient(90deg, #ff6b6b, #ff922b, #e64980)',
                 transform: 'scaleX(0)',
               }} />
          {/* 초록 완료 라인 */}
          <div className="absolute top-7 h-[3px] rounded-r-sm z-[5] max-md:hidden"
               style={{
                 left: 'calc(100% * 11/12 + 32px)', right: 0,
                 background: 'linear-gradient(90deg, #12b886, #51cf66)',
                 opacity: greenLine ? 1 : 0,
                 transition: 'opacity 0.8s ease',
               }} />

          {/* 노드 */}
          <div className="flex justify-between relative z-[2]
                          max-md:flex-col max-md:gap-5">
            {stages.map((stage, i) => {
              const isActive = i === activeIdx;
              const isDone = completed[i];
              return (
                <div key={stage.agent} className="flex flex-col items-center flex-1 relative min-h-[120px]
                                                   max-md:flex-row max-md:gap-3.5 max-md:min-h-0">
                  {/* 아바타 */}
                  <div className="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl
                                  relative z-10 transition-all duration-500 ease-[cubic-bezier(0.34,1.56,0.64,1)]
                                  max-md:w-11 max-md:h-11 max-md:text-xl max-md:shrink-0"
                       style={{
                         background: isDone || isActive ? stage.bg : '#f1f3f5',
                         border: `2px solid ${isDone || isActive ? stage.color : '#dee2e6'}`,
                         transform: isActive ? 'scale(1.15)' : 'scale(1)',
                         boxShadow: isActive ? `0 0 20px ${stage.color}30` : 'none',
                       }}>
                    {stage.emoji}
                    {/* ping 제거 */}
                    {isDone && (
                      <div className="absolute -top-1 -right-1 w-[18px] h-[18px] rounded-full bg-success text-white
                                      text-[10px] flex items-center justify-center font-bold">✓</div>
                    )}
                  </div>

                  {/* 이름 */}
                  <div className="text-xs font-semibold mt-2 max-md:mt-0 max-md:text-sm"
                       style={{ color: isDone || isActive ? stage.color : '#868e96' }}>
                    {stage.agent}
                  </div>

                  {/* 버블 (데스크톱) */}
                  {isActive && (
                    <div className="absolute top-[90px] left-1/2 -translate-x-1/2 px-3.5 py-2.5 rounded-[14px]
                                    text-xs leading-relaxed whitespace-nowrap text-center border-2 z-[2]
                                    max-md:static max-md:translate-x-0 max-md:whitespace-normal max-md:text-left"
                         style={{ background: stage.bg, borderColor: stage.borderColor, color: stage.color, animation: 'fadeUp 0.4s ease both' }}>
                      {phase === 'working' && <>{stage.msg}<span className="animate-pulse"> ●</span></>}
                      {phase === 'done' && stage.done}
                    </div>
                  )}

                  {/* output */}
                  {outputs[i] && !isActive && (
                    <div className="absolute top-[100px] left-1/2 -translate-x-1/2 text-[10px] text-text-muted whitespace-nowrap
                                    max-md:static max-md:translate-x-0">
                      {outputs[i]}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* 하단: 상세 패널 or 완성 카드 */}
      <div className="w-full max-w-[700px] h-[200px] relative overflow-hidden max-md:h-auto max-md:overflow-visible">
        {/* 상세 패널 */}
        {activeIdx >= 0 && !allDone && (
          <AgentDetailPanel stage={stages[activeIdx]} />
        )}

        {/* 완성 카드 */}
        {allDone && (
          <div style={{ animation: 'bounceUp 0.8s cubic-bezier(0.34,1.56,0.64,1) both' }}>
            <div onClick={onComplete}
                 className="inline-flex items-center gap-4 px-9 py-5 rounded-[20px] cursor-pointer
                            bg-gradient-to-r from-brand/10 to-[#ffa8a8]/20 border-2 border-brand/30
                            hover:scale-105 transition-transform max-md:flex-col max-md:text-center max-md:gap-2.5 max-md:px-5 max-md:py-4">
              <span className="text-[40px]">🎮</span>
              <div className="text-left max-md:text-center">
                <div className="text-lg font-bold">게임 완성! <span className="text-sm font-medium text-brand">클릭해서 확인하기 →</span></div>
                <div className="text-sm text-text-muted">6명의 AI 에이전트가 협업해서 게임을 만들었어요</div>
              </div>
            </div>
            <div className="mt-4 flex gap-2 justify-center">
              <button onClick={() => { reset(); setTimeout(run, 100); }}
                      className="px-5 py-2 rounded-xl border-[1.5px] border-border text-text-secondary text-sm font-medium
                                 cursor-pointer hover:border-brand hover:text-brand transition-colors">
                ↻ 다시 보기
              </button>
              <button onClick={onDemo}
                      className="px-7 py-2.5 rounded-[20px] bg-gradient-to-br from-brand to-[#ffa8a8] text-white
                                 text-sm font-bold cursor-pointer border-none hover:scale-105 transition-transform">
                🎮 30초 데모 체험하기
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 스크롤 힌트 */}
      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-1
                      text-text-muted text-xs tracking-wider"
           style={{ animation: 'fadeUp 0.6s ease 2s both' }}>
        <span>스크롤</span>
        <div className="w-4 h-4 border-r-[1.5px] border-b-[1.5px] border-brand"
             style={{ transform: 'rotate(45deg)', animation: 'arrowBounce 1.5s ease-in-out infinite' }} />
      </div>
    </section>
  );
}
