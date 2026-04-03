import { useEffect } from 'react';

interface GamePlayModalProps {
  isOpen: boolean;
  onClose: () => void;
  url: string;
  title: string;
}

export default function GamePlayModal({ isOpen, onClose, url, title }: GamePlayModalProps) {
  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[500] bg-black/80 flex items-center justify-center"
         onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="relative w-[90vw] h-[85vh] max-w-[1200px] bg-black rounded-2xl overflow-hidden shadow-2xl">
        {/* 헤더 */}
        <div className="absolute top-0 left-0 right-0 h-12 bg-black/90 flex items-center justify-between px-4 z-10">
          <span className="text-white text-sm font-semibold">🎮 {title}</span>
          <button onClick={onClose}
                  className="w-8 h-8 rounded-lg bg-white/10 text-white flex items-center justify-center
                             cursor-pointer hover:bg-white/20 transition-colors text-lg">
            ✕
          </button>
        </div>
        {/* 게임 iframe */}
        <iframe src={url} className="w-full h-full border-none pt-12"
                title={title} allow="autoplay" />
      </div>
    </div>
  );
}
