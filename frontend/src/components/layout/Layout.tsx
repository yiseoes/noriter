import type { ReactNode } from 'react';
import Header from './Header';
import MobileNav from './MobileNav';
import type { AuthUser } from '../../types';

interface LayoutProps {
  children: ReactNode;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  activePage: string;
  onNavigate: (page: string) => void;
  onLogin?: () => void;
  user?: AuthUser | null;
  onLogout?: () => void;
}

export default function Layout({ children, theme, onToggleTheme, activePage, onNavigate, onLogin, user, onLogout }: LayoutProps) {
  return (
    <div className="min-h-screen">
      <Header
        theme={theme}
        onToggleTheme={onToggleTheme}
        onNavigate={onNavigate}
        activePage={activePage}
        onLogin={onLogin}
        user={user}
        onLogout={onLogout}
      />
      <main>{children}</main>
      <MobileNav activePage={activePage} onNavigate={onNavigate} />
    </div>
  );
}
