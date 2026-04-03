import { useState } from 'react';
import GameCard from '../game/GameCard';
import EmptyCard from '../game/EmptyCard';
import GamePlayModal from '../modal/GamePlayModal';
import type { Project } from '../../types';

const genreConfig: Record<string, { emoji: string; color: string; gradient: string; label: string }> = {
  ACTION: { emoji: '🧛', color: '#fa5252', gradient: 'linear-gradient(135deg,#ff6b6b,#ffa8a8)', label: '액션' },
  PUZZLE: { emoji: '🧱', color: '#339af0', gradient: 'linear-gradient(135deg,#339af0,#74c0fc)', label: '퍼즐' },
  SHOOTING: { emoji: '🚀', color: '#0ca678', gradient: 'linear-gradient(135deg,#0ca678,#63e6be)', label: '슈팅' },
  ARCADE: { emoji: '👾', color: '#ae3ec9', gradient: 'linear-gradient(135deg,#ae3ec9,#da77f2)', label: '아케이드' },
  STRATEGY: { emoji: '♟️', color: '#e67700', gradient: 'linear-gradient(135deg,#e67700,#ffc078)', label: '전략' },
  ETC: { emoji: '🎲', color: '#6c5ce7', gradient: 'linear-gradient(135deg,#6c5ce7,#a29bfe)', label: '기타' },
};

const TEMPLATES = [
  { id: 'tpl_vampire', name: '뱀파이어 서바이벌', desc: '사방에서 몰려오는 몬스터를 처치하는 서바이벌 액션', genre: 'ACTION', url: '/templates/vampire-survival/', emoji: '🧛' },
  { id: 'tpl_tetris', name: '테트리스', desc: '블록을 회전하고 쌓아 줄을 완성하는 클래식 퍼즐', genre: 'PUZZLE', url: '/templates/tetris/', emoji: '🧱' },
  { id: 'tpl_shooter', name: '스페이스 슈터', desc: '우주를 지켜라! 끝없는 적들을 물리치는 슈팅 게임', genre: 'SHOOTING', url: '/templates/space-shooter/', emoji: '🚀' },
];

interface PlaygroundProps {
  onCreateGame: () => void;
  onSelectProject: (project: Project) => void;
  projects: Project[];
}

export default function Playground({ onCreateGame, onSelectProject, projects }: PlaygroundProps) {
  const [playingGame, setPlayingGame] = useState<{ url: string; title: string } | null>(null);

  return (
    <section id="playground" className="py-20 px-8 max-w-[1200px] mx-auto max-md:py-10 max-md:px-4">
      {/* 템플릿 쇼케이스 */}
      <div className="mb-16">
        <div className="text-sm font-semibold text-brand tracking-widest uppercase">Templates</div>
        <h2 className="text-[28px] font-bold max-md:text-xl">이런 게임을 만들 수 있어요 🎮</h2>
        <p className="text-[15px] text-text-muted mt-1 mb-8">AI가 실제로 만든 게임을 바로 플레이해보세요</p>

        <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-5 max-md:grid-cols-1 max-md:gap-3.5">
          {TEMPLATES.map(tpl => {
            const config = genreConfig[tpl.genre] || genreConfig.ETC;
            return (
              <GameCard
                key={tpl.id}
                emoji={tpl.emoji}
                genre={config.label}
                genreColor={config.color}
                bgGradient={config.gradient}
                name={tpl.name}
                desc={tpl.desc}
                date="템플릿"
                onPlay={() => setPlayingGame({ url: tpl.url, title: tpl.name })}
                onClick={() => setPlayingGame({ url: tpl.url, title: tpl.name })}
              />
            );
          })}
        </div>
      </div>

      {/* 내 프로젝트 */}
      <div>
        <div className="flex justify-between items-end mb-8 max-md:flex-col max-md:items-start max-md:gap-3">
          <div>
            <div className="text-sm font-semibold text-brand tracking-widest uppercase">My Games</div>
            <h2 className="text-[28px] font-bold max-md:text-xl">내가 만든 게임 🎡</h2>
            <p className="text-[15px] text-text-muted mt-1">직접 만든 게임을 확인하고 플레이해보세요</p>
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
                onPlay={() => setPlayingGame({ url: `/api/projects/${project.id}/preview`, title: project.name })}
                onClick={() => onSelectProject(project)}
              />
            );
          })}
          <EmptyCard onClick={onCreateGame} />
        </div>
      </div>

      {/* 게임 플레이 모달 */}
      <GamePlayModal
        isOpen={!!playingGame}
        onClose={() => setPlayingGame(null)}
        url={playingGame?.url || ''}
        title={playingGame?.title || ''}
      />
    </section>
  );
}
