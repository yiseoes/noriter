import ThemeToggle from '../common/ThemeToggle';

interface HeaderProps {
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onNavigate: (page: string) => void;
  activePage: string;
  onLogin?: () => void;
}

export default function Header({ theme, onToggleTheme, onNavigate, activePage, onLogin }: HeaderProps) {
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
        {/* 로고 — 인트로 아웃라인 스타일 */}
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
        <button onClick={onLogin}
                className="px-4 py-1.5 rounded-lg text-sm font-semibold text-brand border border-brand/30
                           cursor-pointer hover:bg-brand-bg transition-colors max-md:px-3 max-md:text-xs">
          로그인
        </button>
      </div>
    </header>
  );
}
