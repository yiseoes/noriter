import { useRef, useCallback, useEffect } from 'react';

interface UseSlideUpOptions {
  isOpen: boolean;
  onClose: () => void;
  threshold?: number;
}

export function useSlideUp({ isOpen, onClose, threshold = 150 }: UseSlideUpOptions) {
  const overlayRef = useRef<HTMLDivElement>(null);
  const cardRef = useRef<HTMLDivElement>(null);
  const dragState = useRef({ isDragging: false, startY: 0, currentY: 0 });

  const onDragStart = useCallback((clientY: number) => {
    const card = cardRef.current;
    if (!card) return;
    dragState.current = { isDragging: true, startY: clientY, currentY: 0 };
    card.style.transition = 'none';
  }, []);

  const onDragMove = useCallback((clientY: number) => {
    const { isDragging, startY } = dragState.current;
    if (!isDragging) return;

    const card = cardRef.current;
    const overlay = overlayRef.current;
    if (!card || !overlay) return;

    let dy = clientY - startY;
    if (dy < 0) dy = 0;
    const resistance = dy > 100 ? 100 + (dy - 100) * 0.3 : dy;
    dragState.current.currentY = dy;

    card.style.transform = `translateY(${resistance}px)`;
    const opacity = Math.max(0, 0.35 - (resistance / window.innerHeight) * 0.5);
    overlay.style.background = `rgba(0,0,0,${opacity})`;
  }, []);

  const onDragEnd = useCallback(() => {
    const { isDragging, currentY } = dragState.current;
    if (!isDragging) return;
    dragState.current.isDragging = false;

    const card = cardRef.current;
    const overlay = overlayRef.current;
    if (!card || !overlay) return;

    card.style.transition = '';
    if (currentY > threshold) {
      onClose();
    } else {
      card.style.transform = 'translateY(0)';
      overlay.style.background = 'rgba(0,0,0,0.35)';
    }
  }, [onClose, threshold]);

  // ESC 키
  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isOpen, onClose]);

  // body 스크롤 잠금
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => { document.body.style.overflow = ''; };
  }, [isOpen]);

  return {
    overlayRef,
    cardRef,
    handlers: {
      onMouseDown: (e: React.MouseEvent) => onDragStart(e.clientY),
      onTouchStart: (e: React.TouchEvent) => onDragStart(e.touches[0].clientY),
    },
    moveHandlers: {
      onMouseMove: (e: MouseEvent) => onDragMove(e.clientY),
      onTouchMove: (e: TouchEvent) => onDragMove(e.touches[0].clientY),
      onMouseUp: () => onDragEnd(),
      onTouchEnd: () => onDragEnd(),
    },
  };
}
