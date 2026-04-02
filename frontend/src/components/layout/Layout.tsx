import type { ReactNode } from 'react';
import Header from './Header';
import MobileNav from './MobileNav';

interface LayoutProps {
  children: ReactNode;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  activePage: string;
  onNavigate: (page: string) => void;
  onLogin?: () => void;
}

export default function Layout({ children, theme, onToggleTheme, activePage, onNavigate, onLogin }: LayoutProps) {
  return (
    <div className="min-h-screen">
      <Header
        theme={theme}
        onToggleTheme={onToggleTheme}
        onNavigate={onNavigate}
        activePage={activePage}
        onLogin={onLogin}
      />
      <main>{children}</main>
      <MobileNav activePage={activePage} onNavigate={onNavigate} />
    </div>
  );
}
