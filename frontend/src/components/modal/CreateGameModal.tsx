import { useState } from 'react';
import ModalOverlay from './ModalOverlay';
import GenreTag from '../common/GenreTag';

const genres = [
  { label: '퍼즐', value: 'PUZZLE' },
  { label: '액션', value: 'ACTION' },
  { label: '아케이드', value: 'ARCADE' },
  { label: '슈팅', value: 'SHOOTING' },
  { label: '전략', value: 'STRATEGY' },
  { label: '기타', value: 'ETC' },
];

interface CreateGameModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: { name?: string; genre?: string; requirement: string }) => void;
  isDemo?: boolean;
  projectCount?: number;
  onLogin?: () => void;
}

export default function CreateGameModal({ isOpen, onClose, onCreate, isDemo = false, projectCount = 0, onLogin }: CreateGameModalProps) {
  const guestLimitReached = !isDemo && projectCount >= 1;
  const [name, setName] = useState('');
  const [genre, setGenre] = useState('');
  const [desc, setDesc] = useState('');

  const canSubmit = desc.length >= 10;

  const handleSubmit = () => {
    onClose();
    onCreate({ name: name || undefined, genre: genre || undefined, requirement: desc });
    setName(''); setGenre(''); setDesc('');
  };

  return (
    <ModalOverlay isOpen={isOpen} onClose={onClose} size="sm"
      title={<>🎮 새 게임 만들기{isDemo && <span className="inline-block ml-2 px-2 py-0.5 rounded-md text-[11px] font-semibold bg-coral-bg text-coral align-middle">DEMO</span>}</>}
      footer={
        <button
          disabled={!canSubmit}
          onClick={handleSubmit}
          className="w-full py-3 bg-gradient-to-br from-brand to-[#a29bfe] text-white
                     rounded-[10px] text-[15px] font-semibold cursor-pointer border-none
                     disabled:opacity-50 disabled:cursor-not-allowed"
        >
          게임 생성 요청
        </button>
      }
    >
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
