interface ThemeToggleProps {
  theme: 'light' | 'dark';
  onToggle: () => void;
}

export default function ThemeToggle({ theme, onToggle }: ThemeToggleProps) {
  return (
    <button
      onClick={onToggle}
      className="w-9 h-9 rounded-lg border border-border-light bg-bg-primary cursor-pointer
                 flex items-center justify-center text-base hover:bg-bg-tertiary transition-colors"
      aria-label="테마 전환"
    >
      {theme === 'light' ? '🌙' : '☀️'}
    </button>
  );
}
