import { useEffect, useRef, useState, type ReactNode } from 'react';

interface ModalOverlayProps {
  isOpen: boolean;
  onClose: () => void;
  size?: 'sm' | 'lg';
  title: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
}

export default function ModalOverlay({ isOpen, onClose, size = 'sm', title, children, footer }: ModalOverlayProps) {
  const [visible, setVisible] = useState(false);
  const [active, setActive] = useState(false);
  const overlayRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen) {
      setVisible(true);
      requestAnimationFrame(() => {
        requestAnimationFrame(() => setActive(true));
      });
    } else if (visible) {
      setActive(false);
      const timer = setTimeout(() => setVisible(false), 450);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  // ESC
  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isOpen, onClose]);

  if (!visible) return null;

  const sizeClass = size === 'lg' ? 'w-[800px]' : 'w-[560px]';

  return (
    <div
      ref={overlayRef}
      onClick={(e) => { if (e.target === overlayRef.current) onClose(); }}
      className={`fixed inset-0 z-[400] flex items-center justify-center transition-all duration-400
        ${active
          ? 'bg-black/50 backdrop-blur-[4px] pointer-events-auto'
          : 'bg-black/0 backdrop-blur-0 pointer-events-none'
        }`}
    >
      <div className={`bg-bg-primary rounded-2xl max-h-[85vh] overflow-y-auto shadow-2xl
                        transition-all duration-400 max-md:!w-[calc(100vw-32px)] max-md:max-h-[90vh]
                        ${sizeClass}
                        ${active
                          ? 'opacity-100 scale-100 translate-y-0'
                          : 'opacity-0 scale-[0.92] translate-y-5'
                        }`}>
        {/* 헤더 */}
        <div className="px-7 pt-6 flex justify-between items-center max-md:px-5 max-md:pt-5">
          <h2 className="text-xl font-bold max-md:text-lg">{title}</h2>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg bg-bg-tertiary flex items-center justify-center
                       text-lg text-text-muted cursor-pointer hover:bg-bg-hover hover:text-text-primary"
          >
            ✕
          </button>
        </div>

        {/* 바디 */}
        <div className="px-7 py-6 max-md:px-5 max-md:py-5">{children}</div>

        {/* 푸터 */}
        {footer && <div className="px-7 pb-6 max-md:px-5 max-md:pb-5">{footer}</div>}
      </div>
    </div>
  );
}
