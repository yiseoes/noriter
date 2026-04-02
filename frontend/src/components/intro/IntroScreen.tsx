import { useState, useCallback, useEffect } from 'react';
import IntroCanvas from './IntroCanvas';

interface IntroScreenProps {
  onDismiss: () => void;
}

export default function IntroScreen({ onDismiss }: IntroScreenProps) {
  const [exiting, setExiting] = useState(false);

  const dismiss = useCallback(() => {
    if (exiting) return;
    setExiting(true);
    setTimeout(onDismiss, 900);
  }, [exiting, onDismiss]);

  // 키보드로도 넘기기
  useEffect(() => {
    const handler = () => dismiss();
    window.addEventListener('keydown', handler, { once: true });
    return () => window.removeEventListener('keydown', handler);
  }, [dismiss]);

  // 로딩바 끝나면 자동 전환 (1.5s 딜레이 + 6s 로딩 = 7.5s)
  useEffect(() => {
    const timer = setTimeout(dismiss, 7800);
    return () => clearTimeout(timer);
  }, [dismiss]);

  return (
    <div
      onClick={dismiss}
      className={`fixed inset-0 z-[10000] bg-bg-secondary flex flex-col items-center justify-center
                  cursor-pointer overflow-hidden
                  ${exiting ? 'pointer-events-none' : ''}`}
      style={{
        transition: 'opacity 0.8s cubic-bezier(0.16,1,0.3,1), transform 0.8s cubic-bezier(0.16,1,0.3,1)',
        opacity: exiting ? 0 : 1,
        transform: exiting ? 'scale(1.05)' : 'scale(1)',
      }}
    >
      <IntroCanvas />

      {/* 구름 */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[min(1000px,92vw)] bottom-0 pointer-events-none z-[2] overflow-hidden">
        {[
          { top: '8%', left: '15%', w: 120, dur: 25, delay: 0 },
          { top: '5%', left: '40%', w: 90, dur: 30, delay: 3 },
          { top: '12%', left: '65%', w: 140, dur: 28, delay: 1 },
          { top: '3%', left: '85%', w: 100, dur: 32, delay: 5 },
          { top: '15%', left: '5%', w: 80, dur: 26, delay: 8 },
        ].map((c, i) => (
          <div
            key={i}
            className="absolute h-[30px] rounded-[50px] max-md:h-[20px]
                       before:content-[''] before:absolute before:top-[-12px] before:left-[20%]
                       before:w-[45%] before:h-[24px] before:rounded-[50px] before:bg-[rgba(108,92,231,0.05)]
                       after:content-[''] after:absolute after:top-[-6px] after:left-[50%]
                       after:w-[35%] after:h-[18px] after:rounded-[50px] after:bg-[rgba(108,92,231,0.04)]
                       max-md:before:h-[16px] max-md:after:h-[12px]"
            style={{
              top: c.top, left: c.left, width: c.w,
              background: 'rgba(108,92,231,0.06)',
              animation: `cloudDrift ${c.dur}s linear ${c.delay}s infinite`,
              opacity: 0,
              animationFillMode: 'both',
            }}
          />
        ))}
      </div>

      {/* 바닥 그라운드 */}
      <div className="absolute bottom-0 left-0 right-0 h-[120px]
                      bg-gradient-to-b from-transparent to-[rgba(108,92,231,0.06)]
                      border-t border-[rgba(108,92,231,0.1)]" />

      {/* 로고 + 타이틀 */}
      <div className="relative z-[2] text-center"
           style={{ animation: 'logoIn 1.2s cubic-bezier(0.16,1,0.3,1) 0.3s both' }}>
        <div className="flex justify-center gap-1 mb-3 max-md:gap-0.5">
          {['놀', '이', '터'].map((char, i) => {
            const n = i + 1;
            const bounceDelay = 0.4 + i * 0.2;
            const wobbleDelay = 1.5 + i * 0.3;
            return (
              <span
                key={char}
                className="inline-block text-[82px] font-extrabold text-transparent
                           max-md:text-[56px] max-[380px]:text-[44px]"
                style={{
                  WebkitTextStroke: `3px ${['#ae3ec9', '#4263eb', '#e64980'][i]}`,
                  animation: `blockBounce${n} 0.6s cubic-bezier(0.34,1.56,0.64,1) ${bounceDelay}s both, blockWobble${n} ${2.3 + i * 0.25}s ease-in-out ${wobbleDelay}s infinite`,
                }}
              >
                {char}
              </span>
            );
          })}
        </div>
        <div className="text-lg text-text-muted max-md:text-[13px]"
             style={{ animation: 'fadeUp 0.8s ease 0.8s both' }}>
          AI Multi-Agent Game Studio
        </div>
      </div>

      {/* 로딩바 */}
      <div className="absolute bottom-[55px] w-[200px] h-[3px] bg-border-light rounded-sm overflow-hidden z-[2]
                      max-md:w-[150px]"
           style={{ animation: 'fadeUp 0.6s ease 1s both' }}>
        <div className="h-full bg-gradient-to-r from-brand to-[#a29bfe] rounded-sm"
             style={{ animation: 'loadProgress 6s ease-in-out 1.5s both' }} />
      </div>

      {/* 클릭 안내 */}
      <div className="absolute bottom-[38px] left-1/2 -translate-x-1/2 z-[3]
                      text-[11px] text-[#bbb] tracking-wider"
           style={{ animation: 'fadeUp 0.6s ease 2s both' }}>
        아무 곳이나 클릭하여 시작하세요
      </div>
    </div>
  );
}
