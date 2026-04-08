import { useState } from 'react';
import ModalOverlay from './ModalOverlay';
import GenreTag from '../common/GenreTag';
import type { UserRole } from '../../types';

const genres = [
  { label: '퍼즐', value: 'PUZZLE' },
  { label: '액션', value: 'ACTION' },
  { label: '아케이드', value: 'ARCADE' },
  { label: '슈팅', value: 'SHOOTING' },
  { label: '전략', value: 'STRATEGY' },
  { label: '기타', value: 'ETC' },
];

const GUEST_LIMIT = 1;
const USER_LIMIT = 3;

interface CreateGameModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: { name?: string; genre?: string; requirement: string }) => void;
  isDemo?: boolean;
  realProjectCount?: number;
  role?: UserRole | null;
  isLoggedIn?: boolean;
}

export default function CreateGameModal({ isOpen, onClose, onCreate, isDemo = false, realProjectCount = 0, role, isLoggedIn }: CreateGameModalProps) {
  const [name, setName] = useState('');
  const [genre, setGenre] = useState('');
  const [desc, setDesc] = useState('');

  const isAdmin = role === 'ADMIN';
  const limit = isAdmin ? Infinity : isLoggedIn ? USER_LIMIT : GUEST_LIMIT;
  const remaining = isAdmin ? Infinity : Math.max(0, limit - realProjectCount);
  const limitReached = !isDemo && remaining <= 0;

  const canSubmit = desc.length >= 10 && !limitReached;

  const handleSubmit = () => {
    onClose();
    onCreate({ name: name || undefined, genre: genre || undefined, requirement: desc });
    setName(''); setGenre(''); setDesc('');
  };

  return (
    <ModalOverlay isOpen={isOpen} onClose={onClose} size="sm"
      title={<>🎮 새 게임 만들기{isDemo && <span className="inline-block ml-2 px-2 py-0.5 rounded-md text-[11px] font-semibold bg-coral-bg text-coral align-middle">DEMO</span>}</>}
      footer={
        <div>
          <button
            disabled={!canSubmit}
            onClick={handleSubmit}
            className="w-full py-3 bg-gradient-to-br from-brand to-[#a29bfe] text-white
                       rounded-[10px] text-[15px] font-semibold cursor-pointer border-none
                       disabled:opacity-50 disabled:cursor-not-allowed"
          >
            게임 생성 요청
          </button>
          {/* 남은 횟수 안내 */}
          {!isDemo && !isAdmin && (
            <div className={`text-center mt-2 text-xs ${remaining <= 1 ? 'text-danger' : 'text-text-muted'}`}>
              {limitReached
                ? (isLoggedIn ? '생성 가능 횟수를 모두 사용했습니다.' : '게스트 무료 체험을 모두 사용했습니다. 로그인해주세요!')
                : `남은 생성 횟수: ${remaining}/${limit}`
              }
            </div>
          )}
        </div>
      }
    >
      {/* 제한 도달 경고 */}
      {limitReached && (
        <div className="mb-4 p-3 rounded-lg bg-danger-bg text-danger text-sm font-medium">
          {isLoggedIn
            ? `게임은 최대 ${USER_LIMIT}개까지 생성할 수 있습니다.`
            : '게스트 모드에서는 1개의 게임만 생성할 수 있습니다. 회원가입하면 더 만들 수 있어요!'
          }
        </div>
      )}

      <div className="mb-5">
        <label className="block text-sm font-semibold text-text-secondary mb-1.5">
          프로젝트명 <span className="font-normal text-text-muted">(선택)</span>
        </label>
        <input
          value={name} onChange={(e) => setName(e.target.value)}
          placeholder="예: 뱀파이어 서바이벌"
          className="w-full bg-bg-primary border border-border rounded-[10px] px-3.5 py-2.5
                     text-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand/10"
        />
      </div>

      <div className="mb-5">
        <label className="block text-sm font-semibold text-text-secondary mb-1.5">
          게임 장르 <span className="font-normal text-text-muted">(선택)</span>
        </label>
        <div className="flex gap-2 flex-wrap max-md:gap-1.5">
          {genres.map(g => (
            <GenreTag key={g.value} label={g.label} selected={genre === g.value} onClick={() => setGenre(genre === g.value ? '' : g.value)} />
          ))}
        </div>
      </div>

      <div>
        <label className="block text-sm font-semibold text-text-secondary mb-1.5">
          어떤 게임을 만들고 싶으세요? <span className="text-danger">*</span>
        </label>
        <textarea
          value={desc} onChange={(e) => setDesc(e.target.value)}
          placeholder="예: 뱀파이어 서바이벌 류 미니게임을 만들어줘..."
          className="w-full min-h-[130px] bg-bg-primary border border-border rounded-[10px] px-3.5 py-2.5
                     text-sm resize-y outline-none focus:border-brand focus:ring-3 focus:ring-brand/10
                     max-md:min-h-[100px]"
        />
        <div className="flex justify-between text-xs text-text-muted mt-1.5">
          <span>최소 10자 이상</span>
          <span className={desc.length >= 10 ? 'text-success' : ''}>
            {desc.length}/10 {desc.length >= 10 && '✓'}
          </span>
        </div>
      </div>
    </ModalOverlay>
  );
}
