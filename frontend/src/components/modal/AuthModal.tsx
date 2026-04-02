import { useState } from 'react';
import ModalOverlay from './ModalOverlay';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function AuthModal({ isOpen, onClose }: AuthModalProps) {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');

  const isLogin = mode === 'login';

  const handleSubmit = () => {
    // TODO: 실제 인증 API 연동
    alert(isLogin ? '로그인 기능은 준비 중이에요!' : '회원가입 기능은 준비 중이에요!');
  };

  return (
    <ModalOverlay isOpen={isOpen} onClose={onClose} size="sm"
      title={isLogin ? '🔑 로그인' : '✨ 회원가입'}
      footer={
        <div>
          <button
            onClick={handleSubmit}
            className="w-full py-3 bg-gradient-to-br from-brand to-[#ffa8a8] text-white
                       rounded-[10px] text-[15px] font-semibold cursor-pointer border-none">
            {isLogin ? '로그인' : '회원가입'}
          </button>
          <div className="text-center mt-3 text-sm text-text-muted">
            {isLogin ? (
              <>계정이 없으신가요? <button onClick={() => setMode('signup')} className="text-brand font-semibold cursor-pointer bg-transparent border-none">회원가입</button></>
            ) : (
              <>이미 계정이 있으신가요? <button onClick={() => setMode('login')} className="text-brand font-semibold cursor-pointer bg-transparent border-none">로그인</button></>
            )}
          </div>
        </div>
      }
    >
      {!isLogin && (
        <div className="mb-5">
          <label className="block text-sm font-semibold text-text-secondary mb-1.5">이름</label>
          <input
            value={name} onChange={(e) => setName(e.target.value)}
            placeholder="이름을 입력하세요"
            className="w-full bg-bg-primary border border-border rounded-[10px] px-3.5 py-2.5
                       text-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand/10"
          />
        </div>
      )}

      <div className="mb-5">
        <label className="block text-sm font-semibold text-text-secondary mb-1.5">이메일</label>
        <input
          type="email"
          value={email} onChange={(e) => setEmail(e.target.value)}
          placeholder="이메일을 입력하세요"
          className="w-full bg-bg-primary border border-border rounded-[10px] px-3.5 py-2.5
                     text-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand/10"
        />
      </div>

      <div>
        <label className="block text-sm font-semibold text-text-secondary mb-1.5">비밀번호</label>
        <input
          type="password"
          value={password} onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호를 입력하세요"
          className="w-full bg-bg-primary border border-border rounded-[10px] px-3.5 py-2.5
                     text-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand/10"
        />
      </div>
    </ModalOverlay>
  );
}
