import { useState } from 'react';
import ModalOverlay from './ModalOverlay';
import { useAuth } from '../../hooks/useAuth';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function AuthModal({ isOpen, onClose }: AuthModalProps) {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState('');

  const { login, signup } = useAuth();
  const isLogin = mode === 'login';
  const isSubmitting = login.isPending || signup.isPending;

  const reset = () => { setEmail(''); setPassword(''); setName(''); setError(''); };

  const handleSubmit = () => {
    setError('');
    if (isLogin) {
      login.mutate({ email, password }, {
        onSuccess: () => { reset(); onClose(); },
        onError: (err: any) => setError(err.response?.data?.message || '로그인에 실패했습니다.'),
      });
    } else {
      signup.mutate({ email, password, name }, {
        onSuccess: () => { reset(); onClose(); },
        onError: (err: any) => setError(err.response?.data?.message || '회원가입에 실패했습니다.'),
      });
    }
  };

  const canSubmit = email && password && (isLogin || name) && !isSubmitting;

  return (
    <ModalOverlay isOpen={isOpen} onClose={() => { reset(); onClose(); }} size="sm"
      title={isLogin ? '🔑 로그인' : '✨ 회원가입'}
      footer={
        <div>
          <button
            disabled={!canSubmit}
            onClick={handleSubmit}
            className="w-full py-3 bg-gradient-to-br from-brand to-[#ffa8a8] text-white
                       rounded-[10px] text-[15px] font-semibold cursor-pointer border-none
                       disabled:opacity-50 disabled:cursor-not-allowed">
            {isSubmitting ? '처리 중...' : isLogin ? '로그인' : '회원가입'}
          </button>
          <div className="text-center mt-3 text-sm text-text-muted">
            {isLogin ? (
              <>계정이 없으신가요? <button onClick={() => { setMode('signup'); setError(''); }} className="text-brand font-semibold cursor-pointer bg-transparent border-none">회원가입</button></>
            ) : (
              <>이미 계정이 있으신가요? <button onClick={() => { setMode('login'); setError(''); }} className="text-brand font-semibold cursor-pointer bg-transparent border-none">로그인</button></>
            )}
          </div>
        </div>
      }
    >
      {error && (
        <div className="mb-4 p-3 rounded-lg bg-danger-bg text-danger text-sm font-medium">
          {error}
        </div>
      )}

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
          onKeyDown={(e) => e.key === 'Enter' && canSubmit && handleSubmit()}
          className="w-full bg-bg-primary border border-border rounded-[10px] px-3.5 py-2.5
                     text-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand/10"
        />
      </div>
    </ModalOverlay>
  );
}
