import { useRef } from 'react';
import HeroSection from '../components/home/HeroSection';
import PipelinePreview from '../components/home/PipelinePreview';
import Playground from '../components/home/Playground';
import { useSnapScroll } from '../hooks/useSnapScroll';
import type { Project } from '../types';

interface HomePageProps {
  onCreateGame: () => void;
  onDemo: () => void;
  onSelectProject: (project: Project) => void;
  projects: Project[];
}

export default function HomePage({ onCreateGame, onDemo, onSelectProject, projects }: HomePageProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  useSnapScroll(containerRef);

  return (
    <div ref={containerRef} className="h-screen overflow-y-auto scroll-smooth">
      <div data-snap-section>
        <HeroSection onCreateGame={onDemo} />
      </div>
      <div data-snap-section>
        <PipelinePreview onComplete={() => projects[0] && onSelectProject(projects[0])} onDemo={onDemo} />
      </div>
      <div data-snap-section className="h-screen overflow-y-auto [-webkit-overflow-scrolling:touch]">
        <Playground onCreateGame={onCreateGame} onSelectProject={onSelectProject} projects={projects} />
        <footer className="py-10 px-8 border-t border-border-light text-center text-text-muted text-sm
                           max-md:py-8 max-md:px-4 max-md:pb-20 max-md:text-[11px]">
          🎪 놀이터 NoriTer — AI 친구들과 함께 만드는 미니게임 놀이터
        </footer>
      </div>
    </div>
  );
}
