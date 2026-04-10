import { useState, useEffect } from 'react';
import ModalOverlay from './ModalOverlay';
import { getApiKeyStatus, getHealth, saveApiKey, validateApiKey } from '../../api/settingsApi';
import type { AuthUser } from '../../types';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  user?: AuthUser | null;
}

export default function SettingsModal({ isOpen, onClose, user }: SettingsModalProps) {
  const [apiKey, setApiKey] = useState('');
  const [keyStatus, setKeyStatus] = useState<{ configured: boolean; maskedKey?: string }>({ configured: false });
  const [health, setHealth] = useState<Record<string, any>>({});
  const [validating, setValidating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    if (!isOpen) return;
    getApiKeyStatus().then(setKeyStatus).catch(() => {});
    getHealth().then(setHealth).catch(() => {});
  }, [isOpen]);

  const handleValidate = async () => {
    if (!apiKey) return;
    setValidating(true);
    setMessage('');
    try {
      const result = await validateApiKey(apiKey);
      setMessage(result.valid ? '✅ 유효한 API 키입니다' : '❌ 유효하지 않은 API 키입니다');
    } catch {
      setMessage('❌ 검증 실패');
    }
    setValidating(false);
  };

  const handleSave = async () => {
    if (!apiKey) return;
    setSaving(true);
    try {
      await saveApiKey(apiKey);
      setMessage('✅ 저장 완료');
      setApiKey('');
      const status = await getApiKeyStatus();
      setKeyStatus(status);
    } catch {
      setMessage('❌ 저장 실패');
    }
    setSaving(false);
  };

  return (
    <ModalOverlay isOpen={isOpen} onClose={onClose} size="sm" title="⚙️ 설정">
      {/* API 키 — ADMIN만 수정 가능 */}
      <div className="bg-bg-secondary border border-border-light rounded-xl p-5 mb-4 max-md:p-4">
        <h3 className="text-[15px] font-semibold mb-3.5">Claude API 키</h3>
        <div className="flex items-center gap-2 mb-3.5">
          <span className={`w-2 h-2 rounded-full ${keyStatus.configured ? 'bg-success' : 'bg-danger'}`} />
          <span className="text-sm">{keyStatus.configured ? '설정됨' : '미설정'}</span>
          {keyStatus.maskedKey && <span className="text-xs text-text-muted font-mono">{keyStatus.maskedKey}</span>}
        </div>

        {isAdmin ? (
          <>
            <div className="mb-3">
              <label className="block text-sm font-semibold text-text-secondary mb-1.5">새 API 키</label>
              <input
                type="password"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                placeholder="sk-ant-api03-..."
                className="w-full bg-bg-primary border border-border rounded-[10px] px-3.5 py-2.5
                           text-sm outline-none focus:border-brand focus:ring-3 focus:ring-brand/10"
              />
            </div>
            {message && <div className="text-sm mb-3">{message}</div>}
            <div className="flex gap-2">
              <button onClick={handleValidate} disabled={validating || !apiKey}
                      className="px-4 py-2 rounded-lg text-sm font-medium border border-border bg-bg-primary text-text-secondary cursor-pointer hover:bg-bg-tertiary disabled:opacity-50">
                {validating ? '검증 중...' : '유효성 검증'}
              </button>
              <button onClick={handleSave} disabled={saving || !apiKey}
                      className="px-5 py-2.5 bg-gradient-to-br from-brand to-[#ffa8a8] text-white rounded-[10px] text-sm font-semibold cursor-pointer border-none disabled:opacity-50">
                {saving ? '저장 중...' : '저장'}
              </button>
            </div>
          </>
        ) : (
          <div className="text-sm text-text-muted py-2">
            API 키 설정은 관리자만 변경할 수 있어요.
          </div>
        )}
      </div>

      {/* 시스템 상태 — 누구나 조회 가능 */}
      <div className="bg-bg-secondary border border-border-light rounded-xl p-5 max-md:p-4">
        <h3 className="text-[15px] font-semibold mb-3.5">시스템 상태</h3>
        {[
          { label: '서버', status: health.components?.server?.status },
          { label: '데이터베이스', status: health.components?.database?.status },
          { label: 'Claude API', status: health.components?.claudeApi?.status },
          { label: '활성 파이프라인', status: `${health.components?.activePipelines?.count ?? 0}개` },
        ].map(item => (
          <div key={item.label} className="flex items-center gap-2.5 py-1.5 text-sm max-md:text-[13px]">
            <span className="text-text-secondary min-w-[120px]">{item.label}</span>
            {item.status === 'UP' && <span className="w-2 h-2 bg-success rounded-full" />}
            {item.status === 'DOWN' && <span className="w-2 h-2 bg-danger rounded-full" />}
            <span>{item.status || '—'}</span>
          </div>
        ))}
      </div>
    </ModalOverlay>
  );
}
