import { useState, useRef, useEffect } from 'react';
import ThemeToggle from '../common/ThemeToggle';
import type { AuthUser } from '../../types';

interface HeaderProps {
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onNavigate: (page: string) => void;
  activePage: string;
  onLogin?: () => void;
  user?: AuthUser | null;
  onLogout?: () => void;
}

export default function Header({ theme, onToggleTheme, onNavigate, activePage, onLogin, user, onLogout }: HeaderProps) {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const navItems = [
    { key: 'home', label: '홈' },
    { key: 'projects', label: '내 프로젝트' },
    { key: 'audit', label: '감사 로그' },
    { key: 'settings', label: '설정' },
  ];

  return (
    <header className="fixed top-0 left-0 right-0 z-50 h-[60px] px-8 flex items-center justify-between
                        bg-white/90 backdrop-blur-lg border-b border-border-light
                        max-md:h-[52px] max-md:px-4">
      <div className="flex items-center gap-9 max-md:gap-3">
        <button
          onClick={() => onNavigate('home')}
          className="flex items-center gap-[3px] font-extrabold text-xl cursor-pointer
                     transition-transform duration-300 hover:scale-105 hover:-rotate-2"
        >
          <span style={{ color: 'transparent', WebkitTextStroke: '1.2px #ae3ec9' }}>놀</span>
          <span style={{ color: 'transparent', WebkitTextStroke: '1.2px #4263eb' }}>이</span>
          <span style={{ color: 'transparent', WebkitTextStroke: '1.2px #e64980' }}>터</span>
        </button>

        <nav className="flex gap-1 max-md:hidden">
          {navItems.map(item => (
            <button
              key={item.key}
              onClick={() => onNavigate(item.key)}
              className={`px-4 py-2 rounded-lg text-sm font-medium cursor-pointer transition-colors
                ${activePage === item.key
                  ? 'bg-coral-bg text-coral'
                  : 'text-text-secondary hover:bg-bg-tertiary hover:text-text-primary'
                }`}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </div>

      <div className="flex items-center gap-3 max-md:gap-2">
        <div className="flex items-center gap-1.5 text-sm text-text-muted max-md:hidden">
          <span className="w-2 h-2 bg-success rounded-full" />
          시스템 정상
        </div>
        <ThemeToggle theme={theme} onToggle={onToggleTheme} />

        {user ? (
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setDropdownOpen(!dropdownOpen)}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg cursor-pointer
                         hover:bg-bg-tertiary transition-colors"
            >
              <span className="w-7 h-7 rounded-full bg-gradient-to-br from-brand to-[#ffa8a8]
                               flex items-center justify-center text-white text-xs font-bold">
                {user.name.charAt(0)}
              </span>
              <span className="text-sm font-medium text-text-primary max-md:hidden">{user.name}</span>
              {user.role === 'ADMIN' && (
                <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-brand/10 text-brand max-md:hidden">ADMIN</span>
              )}
            </button>

            {dropdownOpen && (
              <div className="absolute right-0 top-full mt-1 w-44 bg-bg-primary border border-border
                              rounded-xl shadow-lg overflow-hidden z-50">
                <div className="px-4 py-3 border-b border-border">
                  <div className="flex items-center gap-1.5">
                    <span className="text-sm font-semibold text-text-primary">{user.name}</span>
                    {user.role === 'ADMIN' && (
                      <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-brand/10 text-brand">ADMIN</span>
                    )}
                  </div>
                  <div className="text-xs text-text-muted truncate">{user.email}</div>
                </div>
                <button
                  onClick={() => { setDropdownOpen(false); onLogout?.(); }}
                  className="w-full px-4 py-2.5 text-left text-sm text-danger cursor-pointer
                             hover:bg-bg-tertiary transition-colors border-none bg-transparent"
                >
                  로그아웃
                </button>
              </div>
            )}
          </div>
        ) : (
          <button onClick={onLogin}
                  className="px-4 py-1.5 rounded-lg text-sm font-semibold text-brand border border-brand/30
                             cursor-pointer hover:bg-brand-bg transition-colors max-md:px-3 max-md:text-xs">
            로그인
          </button>
        )}
      </div>
    </header>
  );
}
