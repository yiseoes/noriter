interface MobileNavProps {
  activePage: string;
  onNavigate: (page: string) => void;
}

const items = [
  { key: 'home', icon: '🏠', label: '홈' },
  { key: 'projects', icon: '📂', label: '프로젝트' },
  { key: 'create', icon: '🎮', label: '생성' },
  { key: 'audit', icon: '📋', label: '감사로그' },
  { key: 'settings', icon: '⚙️', label: '설정' },
];

export default function MobileNav({ activePage, onNavigate }: MobileNavProps) {
  return (
    <nav className="hidden max-md:flex fixed bottom-0 left-0 right-0 z-50
                     bg-white/95 backdrop-blur-lg border-t border-border-light
                     justify-around py-2">
      {items.map(item => (
        <button
          key={item.key}
          onClick={() => onNavigate(item.key)}
          className={`flex flex-col items-center gap-0.5 px-2 py-1 text-[10px] cursor-pointer
            ${activePage === item.key ? 'text-brand' : 'text-text-muted'}`}
        >
          <span className="text-lg">{item.icon}</span>
          {item.label}
        </button>
      ))}
    </nav>
  );
}
