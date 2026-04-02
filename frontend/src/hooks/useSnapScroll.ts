import { useEffect, useRef, useCallback } from 'react';

export function useSnapScroll(containerRef: React.RefObject<HTMLDivElement | null>) {
  const isScrolling = useRef(false);
  const currentSection = useRef(0);
  const totalSections = useRef(0);

  const scrollToSection = useCallback((index: number) => {
    const container = containerRef.current;
    if (!container || isScrolling.current) return;

    const sections = container.querySelectorAll<HTMLElement>('[data-snap-section]');
    totalSections.current = sections.length;

    if (index < 0 || index >= sections.length) return;

    isScrolling.current = true;
    currentSection.current = index;

    sections[index].scrollIntoView({ behavior: 'smooth' });

    // 스크롤 완료 대기
    setTimeout(() => {
      isScrolling.current = false;
    }, 800);
  }, [containerRef]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const sections = container.querySelectorAll('[data-snap-section]');
    totalSections.current = sections.length;

    let wheelTimeout: number;
    let accumulated = 0;
    const THRESHOLD = 50;

    const onWheel = (e: WheelEvent) => {
      // 마지막 섹션(자유 스크롤) 안에서는 패스
      const lastSection = sections[sections.length - 1] as HTMLElement;
      if (lastSection && lastSection.contains(e.target as Node)) {
        // 마지막 섹션 맨 위에서 위로 스크롤하면 이전 섹션으로
        if (e.deltaY < 0 && lastSection.scrollTop <= 0) {
          e.preventDefault();
          scrollToSection(currentSection.current - 1);
        }
        return;
      }

      e.preventDefault();
      accumulated += e.deltaY;

      clearTimeout(wheelTimeout);
      wheelTimeout = window.setTimeout(() => { accumulated = 0; }, 200);

      if (Math.abs(accumulated) < THRESHOLD) return;

      if (accumulated > 0) {
        scrollToSection(currentSection.current + 1);
      } else {
        scrollToSection(currentSection.current - 1);
      }
      accumulated = 0;
    };

    // 터치 지원
    let touchStartY = 0;
    const onTouchStart = (e: TouchEvent) => {
      touchStartY = e.touches[0].clientY;
    };
    const onTouchEnd = (e: TouchEvent) => {
      const lastSection = sections[sections.length - 1] as HTMLElement;
      if (lastSection && lastSection.contains(e.target as Node)) {
        if (lastSection.scrollTop > 0) return;
      }

      const dy = touchStartY - e.changedTouches[0].clientY;
      if (Math.abs(dy) < 50) return;

      if (dy > 0) {
        scrollToSection(currentSection.current + 1);
      } else {
        scrollToSection(currentSection.current - 1);
      }
    };

    container.addEventListener('wheel', onWheel, { passive: false });
    container.addEventListener('touchstart', onTouchStart, { passive: true });
    container.addEventListener('touchend', onTouchEnd, { passive: true });

    return () => {
      container.removeEventListener('wheel', onWheel);
      container.removeEventListener('touchstart', onTouchStart);
      container.removeEventListener('touchend', onTouchEnd);
    };
  }, [containerRef, scrollToSection]);

  return { scrollToSection };
}
