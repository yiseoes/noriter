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
  { id: 'tpl_vampire', name: '뱀파이어 서바이벌', desc: '횡스크롤 RPG — 캐릭터 선택, 6개 맵, 레벨업, 스킬, 보스전', genre: 'ACTION', url: '/templates/vampire-survival/index.html', emoji: '🧛',
    prompt: '메이플스토리 같은 횡스크롤 RPG를 만들어줘. 뱀파이어 세계관, 캐릭터 선택, 맵 이동, 레벨업 시 스킬 선택, 보스 몬스터, 아이템 드롭, 인벤토리가 있었으면 좋겠어.' },
  { id: 'tpl_tetris', name: '테트리스', desc: '클래식 퍼즐 — 홀드, 고스트, 하드드롭, 레벨 시스템', genre: 'PUZZLE', url: '/templates/tetris/index.html', emoji: '🧱',
    prompt: '테트리스 게임을 만들어줘. 7종 블록, 홀드 기능, 고스트 피스, 하드드롭, 소프트드롭, 회전+벽차기, 라인 클리어 시 점수, 레벨별 속도 증가가 있었으면 좋겠어.' },
  { id: 'tpl_shooter', name: '스페이스 슈터', desc: '종스크롤 슈팅 — 웨이브, 파워업, 보스전', genre: 'SHOOTING', url: '/templates/space-shooter/index.html', emoji: '🚀',
    prompt: '우주 슈팅 게임을 만들어줘. 웨이브 시스템으로 점점 강해지는 적, 파워업 아이템(트리플샷, 쉴드, 스피드, 폭탄), 보스 몬스터, 라이프 시스템, 점수판이 있었으면 좋겠어.' },
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
                prompt={tpl.prompt}
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
                onPlay={() => {
                  // 데모 프로젝트면 장르별 템플릿 게임, 실제면 백엔드 미리보기
                  const templateMap: Record<string, string> = {
                    ACTION: '/templates/vampire-survival/index.html',
                    PUZZLE: '/templates/tetris/index.html',
                    SHOOTING: '/templates/space-shooter/index.html',
                  };
                  const url = project.demo && templateMap[project.genre]
                    ? templateMap[project.genre]
                    : `/api/projects/${project.id}/preview`;
                  setPlayingGame({ url, title: project.name });
                }}
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
