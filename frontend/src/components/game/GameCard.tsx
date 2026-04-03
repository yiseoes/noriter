interface GameCardProps {
  emoji: string;
  genre: string;
  genreColor: string;
  bgGradient: string;
  name: string;
  desc: string;
  date: string;
  isDemo?: boolean;
  prompt?: string;
  onPlay?: () => void;
  onClick?: () => void;
}

export default function GameCard({ emoji, genre, genreColor, bgGradient, name, desc, date, isDemo, prompt, onPlay, onClick }: GameCardProps) {
  return (
    <div
      onClick={onClick}
      className="bg-bg-primary border-2 border-border-light rounded-[20px] overflow-hidden cursor-pointer
                 hover:border-brand hover:-translate-y-2 hover:-rotate-1 hover:shadow-xl hover:shadow-brand/10
                 transition-all duration-400 ease-[cubic-bezier(0.34,1.56,0.64,1)]"
    >
      <div
        className="h-[180px] flex items-center justify-center text-5xl relative max-md:h-[140px]"
        style={{ background: bgGradient }}
      >
        {emoji}
        {isDemo && (
          <span className="absolute top-3 right-3 px-2 py-0.5 rounded-md text-[10px] font-bold bg-black/50 text-white backdrop-blur-sm">
            DEMO
          </span>
        )}
      </div>

      <div className="px-5 pt-4 pb-5">
        <span
          className="inline-block px-2.5 py-0.5 rounded-lg text-[11px] font-semibold mb-2"
          style={{ background: `${genreColor}15`, color: genreColor }}
        >
          {genre}
        </span>
        <div className="text-[17px] font-bold mb-1 max-md:text-[15px]">{name}</div>
        <div className="text-sm text-text-muted mb-3.5 leading-relaxed max-md:text-xs">{desc}</div>
        <div className="flex justify-between items-center">
          <span className="text-xs text-text-muted">{date}</span>
          <button
            onClick={(e) => { e.stopPropagation(); onPlay?.(); }}
            className="px-5 py-2 rounded-[14px] bg-gradient-to-br from-brand to-[#ffa8a8] text-white
                       text-sm font-semibold cursor-pointer border-none
                       hover:scale-108 hover:shadow-lg hover:shadow-brand/30
                       transition-all duration-300 ease-[cubic-bezier(0.34,1.56,0.64,1)]"
          >
            플레이 ▶
          </button>
        </div>
        {prompt && (
          <div className="mt-3 pt-3 border-t border-border-light">
            <div className="text-[10px] font-semibold text-text-muted mb-1">💬 프롬프트 예시</div>
            <div className="text-[11px] text-text-secondary leading-relaxed bg-bg-secondary rounded-lg px-3 py-2 italic">
              "{prompt}"
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
