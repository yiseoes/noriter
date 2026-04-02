import { useEffect, useRef, useState, useCallback, type ReactNode } from 'react';

interface SlideUpModalProps {
  isOpen: boolean;
  onClose: () => void;
  children: ReactNode;
  headerLeft?: ReactNode;
  headerRight?: ReactNode;
}

export default function SlideUpModal({ isOpen, onClose, children, headerLeft, headerRight }: SlideUpModalProps) {
  const [visible, setVisible] = useState(false);
  const [active, setActive] = useState(false);
  const [closing, setClosing] = useState(false);
  const overlayRef = useRef<HTMLDivElement>(null);
  const cardRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef({ isDragging: false, startY: 0, currentY: 0 });

  // 열기
  useEffect(() => {
    if (isOpen) {
      setVisible(true);
      setClosing(false);
      requestAnimationFrame(() => {
        requestAnimationFrame(() => setActive(true));
      });
      document.body.style.overflow = 'hidden';
    } else if (visible) {
      // 외부에서 isOpen=false로 닫힐 때
      handleClose();
    }
  }, [isOpen]);

  // 닫기 (double rAF 패턴)
  const handleClose = useCallback(() => {
    const card = cardRef.current;
    const overlay = overlayRef.current;
    if (card) {
      card.style.transform = '';
      card.style.transition = '';
    }
    if (overlay) overlay.style.background = '';

    setClosing(true);
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        setActive(false);
      });
    });
    document.body.style.overflow = '';
    setTimeout(() => {
      setVisible(false);
      setClosing(false);
      onClose();
    }, 550);
  }, [onClose]);

  // ESC
  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') handleClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isOpen, handleClose]);

  // 드래그
  const onDragStart = useCallback((clientY: number) => {
    const card = cardRef.current;
    if (!card) return;
    dragRef.current = { isDragging: true, startY: clientY, currentY: 0 };
    card.style.transition = 'none';
  }, []);

  const onDragMove = useCallback((clientY: number) => {
    if (!dragRef.current.isDragging) return;
    const card = cardRef.current;
    const overlay = overlayRef.current;
    if (!card || !overlay) return;

    let dy = clientY - dragRef.current.startY;
    if (dy < 0) dy = 0;
    const resistance = dy > 100 ? 100 + (dy - 100) * 0.3 : dy;
    dragRef.current.currentY = dy;

    card.style.transform = `translateY(${resistance}px)`;
    overlay.style.background = `rgba(0,0,0,${Math.max(0, 0.35 - (resistance / window.innerHeight) * 0.5)})`;
  }, []);

  const onDragEnd = useCallback(() => {
    if (!dragRef.current.isDragging) return;
    dragRef.current.isDragging = false;

    const card = cardRef.current;
    const overlay = overlayRef.current;
    if (!card || !overlay) return;

    card.style.transition = '';
    if (dragRef.current.currentY > 150) {
      handleClose();
    } else {
      card.style.transform = 'translateY(0)';
      overlay.style.background = 'rgba(0,0,0,0.35)';
    }
  }, [handleClose]);

  // 글로벌 이벤트
  useEffect(() => {
    if (!visible) return;
    const move = (e: MouseEvent) => onDragMove(e.clientY);
    const touchMove = (e: TouchEvent) => onDragMove(e.touches[0].clientY);
    const end = () => onDragEnd();

    window.addEventListener('mousemove', move);
    window.addEventListener('touchmove', touchMove, { passive: true });
    window.addEventListener('mouseup', end);
    window.addEventListener('touchend', end);
    return () => {
      window.removeEventListener('mousemove', move);
      window.removeEventListener('touchmove', touchMove);
      window.removeEventListener('mouseup', end);
      window.removeEventListener('touchend', end);
    };
  }, [visible, onDragMove, onDragEnd]);

  // cleanup
  useEffect(() => {
    return () => { document.body.style.overflow = ''; };
  }, []);

  if (!visible) return null;

  return (
    <div
      ref={overlayRef}
      className={`fixed inset-0 z-[300] transition-colors
        ${closing ? 'duration-[450ms] ease-[cubic-bezier(0.32,0,0.67,0)]' : 'duration-[600ms] ease-[cubic-bezier(0.16,1,0.3,1)]'}
        ${active ? 'bg-black/35 pointer-events-auto' : 'bg-black/0 pointer-events-none'}`}
    >
      <div
        ref={cardRef}
        className={`fixed left-0 right-0 bottom-0 h-[calc(100vh-40px)] bg-bg-primary
                     rounded-t-2xl shadow-2xl flex flex-col overflow-hidden will-change-transform
                     max-md:h-[calc(100vh-20px)] max-md:rounded-t-[14px]
                     ${closing
                       ? 'transition-transform duration-[450ms] ease-[cubic-bezier(0.32,0,0.67,0)]'
                       : 'transition-transform duration-700 ease-[cubic-bezier(0.22,1,0.36,1)]'
                     }
                     ${active ? 'translate-y-0' : 'translate-y-full'}`}
      >
        {/* 핸들 */}
        <div
          className="w-9 h-1 bg-border rounded-full mx-auto mt-2.5 shrink-0 cursor-grab touch-none select-none"
          onMouseDown={(e) => onDragStart(e.clientY)}
          onTouchStart={(e) => onDragStart(e.touches[0].clientY)}
        />

        {/* 헤더 */}
        <div
          className="px-7 py-3 border-b border-border-light flex justify-between items-center shrink-0
                     max-md:px-4 max-md:py-2.5 select-none"
          onMouseDown={(e) => onDragStart(e.clientY)}
          onTouchStart={(e) => onDragStart(e.touches[0].clientY)}
        >
          <button
            onClick={handleClose}
            className="flex items-center gap-2 text-sm text-text-secondary font-medium
                       px-3.5 py-2 rounded-lg hover:bg-bg-tertiary hover:text-text-primary cursor-pointer transition-colors"
          >
            ← 돌아가기
          </button>
          {headerRight}
        </div>

        {/* 바디 */}
        <div className="flex-1 overflow-y-auto px-7 py-6 max-w-[1200px] mx-auto w-full
                        max-md:px-4 max-md:py-4 [-webkit-overflow-scrolling:touch]">
          {children}
        </div>
      </div>
    </div>
  );
}
