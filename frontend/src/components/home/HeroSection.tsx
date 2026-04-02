import { useEffect, useRef } from 'react';

interface HeroSectionProps {
  onCreateGame: () => void;
}

const EMOJIS = ['🎮','🧩','🚀','⭐','🎨','🎵','🎲','✨','🎯','🕹️','👾','🎪','🧸','🎠','🎡','💫'];

export default function HeroSection({ onCreateGame }: HeroSectionProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  // 떠다니는 이모지 파티클
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const sizes = [14,16,18,20,24,26,28,32,36,40,44,48];
    const particles: { el: HTMLSpanElement; x: number; y: number; vx: number; vy: number; rot: number; vr: number; wp: number; ws: number; wa: number }[] = [];

    for (let i = 0; i < 12; i++) {
      const el = document.createElement('span');
      el.className = 'absolute pointer-events-none will-change-transform';
      el.textContent = EMOJIS[Math.floor(Math.random() * EMOJIS.length)];
      const size = sizes[i % sizes.length];
      el.style.fontSize = size + 'px';
      el.style.opacity = String(size < 24 ? 0.2 + Math.random() * 0.15 : 0.3 + Math.random() * 0.25);
      container.appendChild(el);

      particles.push({
        el, x: Math.random() * 100, y: Math.random() * 100,
        vx: (Math.random() - 0.5) * 0.3, vy: (Math.random() - 0.5) * 0.2 - 0.1,
        rot: Math.random() * 360, vr: (Math.random() - 0.5) * 1.5,
        wp: Math.random() * Math.PI * 2, ws: 0.01 + Math.random() * 0.02, wa: 5 + Math.random() * 15,
      });
    }

    let id: number;
    function animate() {
      particles.forEach(p => {
        p.wp += p.ws;
        p.x += p.vx + Math.sin(p.wp) * 0.08;
        p.y += p.vy + Math.cos(p.wp * 0.7) * 0.05;
        p.rot += p.vr;
        if (p.x > 105) p.x = -5; if (p.x < -5) p.x = 105;
        if (p.y > 105) p.y = -5; if (p.y < -5) p.y = 105;
        const wx = Math.sin(p.wp) * p.wa;
        const wy = Math.cos(p.wp * 1.3) * p.wa * 0.6;
        p.el.style.transform = `translate(${wx}px, ${wy}px) rotate(${p.rot}deg)`;
        p.el.style.left = p.x + '%';
        p.el.style.top = p.y + '%';
      });
      id = requestAnimationFrame(animate);
    }
    id = requestAnimationFrame(animate);
    return () => { cancelAnimationFrame(id); container.innerHTML = ''; };
  }, []);

  return (
    <section className="min-h-screen flex flex-col justify-center items-center text-center
                        px-8 relative overflow-hidden snap-start
                        max-md:px-5">
      {/* 이모지 파티클 */}
      <div ref={containerRef} className="absolute inset-0 pointer-events-none z-0 overflow-hidden" />

      {/* 뱃지 */}
      <div className="relative z-10 inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full
                      bg-brand-bg text-brand text-sm font-semibold mb-5"
           style={{ animation: 'badgeDrop 0.7s cubic-bezier(0.34,1.56,0.64,1) 0.2s both, badgeWiggle 3s ease-in-out 1.5s infinite' }}>
        🎪 놀이터에 오신 걸 환영해요!
      </div>

      {/* 제목 */}
      <h1 className="relative z-10 text-[52px] font-extrabold leading-tight mb-5 max-md:text-[28px]">
        <div style={{ animation: 'slideFromLeft 0.8s cubic-bezier(0.16,1,0.3,1) 0.5s both' }}>
          <span className="bg-clip-text text-transparent max-md:text-[28px]"
                style={{ backgroundImage: 'linear-gradient(135deg,#ff6b6b,#ff922b,#e64980)', backgroundSize: '200% 200%', animation: 'gradientShift 4s ease infinite' }}>
            상상하면,
          </span>
        </div>
        <div style={{ animation: 'bounceUp 0.8s cubic-bezier(0.34,1.56,0.64,1) 0.8s both' }}>
          게임이 돼요{' '}
          <img src="/놀람.png" alt="" className="inline-block w-14 h-14 align-middle max-md:w-9 max-md:h-9"
               style={{ animation: 'inlineEmojiBounce 2s ease-in-out 1.2s infinite' }} />
        </div>
      </h1>

      {/* 설명 */}
      <p className="relative z-10 text-lg text-text-secondary max-w-[480px] mx-auto mb-10 leading-relaxed max-md:text-sm"
         style={{ animation: 'slideFromRight 0.7s cubic-bezier(0.16,1,0.3,1) 1.1s both' }}>
        AI 친구들이 당신의 상상을 게임으로 만들어줘요.<br />
        어떤 게임을 만들어볼까요?
      </p>

      {/* 버튼 */}
      <div className="relative z-10 flex gap-3.5 justify-center max-md:flex-col max-md:gap-2 max-md:w-full">
        <button
          onClick={onCreateGame}
          className="px-9 py-4 rounded-[20px] text-base font-bold cursor-pointer border-none
                     bg-gradient-to-br from-brand to-[#ffa8a8] text-white shadow-lg shadow-brand/30
                     hover:-translate-y-1 hover:scale-105 hover:-rotate-1 hover:shadow-xl hover:shadow-brand/40
                     transition-all duration-300 ease-[cubic-bezier(0.34,1.56,0.64,1)]
                     max-md:w-full max-md:text-center"
          style={{ animation: 'btnPopUp 0.6s cubic-bezier(0.34,1.56,0.64,1) 1.4s both' }}
        >
          🎮 30초 데모 체험하기
        </button>
        <a
          href="#playground"
          className="px-9 py-4 rounded-[20px] text-base font-bold border-2 border-border-light
                     bg-bg-primary text-text-primary no-underline
                     hover:border-brand hover:text-brand hover:-translate-y-0.5 hover:scale-103 hover:rotate-1
                     transition-all duration-300 ease-[cubic-bezier(0.34,1.56,0.64,1)]
                     max-md:w-full max-md:text-center"
          style={{ animation: 'btnPopUp 0.6s cubic-bezier(0.34,1.56,0.64,1) 1.6s both' }}
        >
          👀 구경하기
        </a>
      </div>

      {/* 스크롤 힌트 */}
      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-1
                      text-text-muted text-xs tracking-wider z-10"
           style={{ animation: 'fadeUp 0.6s ease 2.5s both' }}>
        <span>스크롤</span>
        <div className="w-4 h-4 border-r-[1.5px] border-b-[1.5px] border-brand"
             style={{ transform: 'rotate(45deg)', animation: 'arrowBounce 1.5s ease-in-out infinite' }} />
      </div>
    </section>
  );
}
