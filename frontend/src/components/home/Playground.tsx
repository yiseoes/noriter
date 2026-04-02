import GameCard from '../game/GameCard';
import EmptyCard from '../game/EmptyCard';
import type { Project } from '../../types';

const genreConfig: Record<string, { emoji: string; color: string; gradient: string; label: string }> = {
  ACTION: { emoji: '🧛', color: '#fa5252', gradient: 'linear-gradient(135deg,#ff6b6b,#ffa8a8)', label: '액션' },
  PUZZLE: { emoji: '🧱', color: '#339af0', gradient: 'linear-gradient(135deg,#339af0,#74c0fc)', label: '퍼즐' },
  SHOOTING: { emoji: '🚀', color: '#0ca678', gradient: 'linear-gradient(135deg,#0ca678,#63e6be)', label: '슈팅' },
  ARCADE: { emoji: '👾', color: '#ae3ec9', gradient: 'linear-gradient(135deg,#ae3ec9,#da77f2)', label: '아케이드' },
  STRATEGY: { emoji: '♟️', color: '#e67700', gradient: 'linear-gradient(135deg,#e67700,#ffc078)', label: '전략' },
  ETC: { emoji: '🎲', color: '#6c5ce7', gradient: 'linear-gradient(135deg,#6c5ce7,#a29bfe)', label: '기타' },
};

interface PlaygroundProps {
  onCreateGame: () => void;
  onSelectProject: (project: Project) => void;
  projects: Project[];
}

export default function Playground({ onCreateGame, onSelectProject, projects }: PlaygroundProps) {
  return (
    <section id="playground" className="py-20 px-8 max-w-[1200px] mx-auto max-md:py-10 max-md:px-4">
      <div className="flex justify-between items-end mb-8 max-md:flex-col max-md:items-start max-md:gap-3">
        <div>
          <div className="text-sm font-semibold text-brand tracking-widest uppercase">Playground</div>
          <h2 className="text-[28px] font-bold max-md:text-xl">놀이터 🎡</h2>
          <p className="text-[15px] text-text-muted mt-1">AI 친구들이 만든 게임을 바로 플레이해보세요!</p>
        </div>
        <button
          onClick={onCreateGame}
          className="px-6 py-2.5 rounded-[20px] text-sm font-bold cursor-pointer
                     bg-gradient-to-br from-brand to-[#ffa8a8] text-white shadow-lg shadow-brand/30
                     hover:-translate-y-0.5 hover:scale-103 transition-all duration-300"
        >
          + 게임 만들기
        </button>
      </div>

      <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-5 max-md:grid-cols-1 max-md:gap-3.5">
        {projects.map(project => {
          const config = genreConfig[project.genre] || genreConfig.ETC;
          return (
            <GameCard
              key={project.id}
              emoji={config.emoji}
              genre={config.label}
              genreColor={config.color}
              bgGradient={config.gradient}
              name={project.name}
              desc={project.requirement}
              date={project.createdAt}
              isDemo={project.demo}
              onClick={() => onSelectProject(project)}
            />
          );
        })}
        <EmptyCard onClick={onCreateGame} />
      </div>
    </section>
  );
}
