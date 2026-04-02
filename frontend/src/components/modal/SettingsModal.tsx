import { useState } from 'react';
import ModalOverlay from './ModalOverlay';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function SettingsModal({ isOpen, onClose }: SettingsModalProps) {
  const [apiKey, setApiKey] = useState('');

  return (
    <ModalOverlay isOpen={isOpen} onClose={onClose} size="sm" title="⚙️ 설정">
      {/* API 키 */}
      <div className="bg-bg-secondary border border-border-light rounded-xl p-5 mb-4 max-md:p-4">
        <h3 className="text-[15px] font-semibold mb-3.5">Claude API 키</h3>
        <div className="flex items-center gap-2 mb-3.5">
          <span className="w-2 h-2 bg-success rounded-full" />
          <span className="text-sm">설정됨</span>
          <span className="text-xs text-text-muted font-mono">sk-ant-a***...****</span>
        </div>
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
        <div className="flex gap-2">
          <button className="px-4 py-2 rounded-lg text-sm font-medium border border-border bg-bg-primary text-text-secondary cursor-pointer hover:bg-bg-tertiary">
            유효성 검증
          </button>
          <button className="px-5 py-2.5 bg-gradient-to-br from-brand to-[#a29bfe] text-white rounded-[10px] text-sm font-semibold cursor-pointer border-none">
            저장
          </button>
        </div>
      </div>

      {/* 시스템 상태 */}
      <div className="bg-bg-secondary border border-border-light rounded-xl p-5 max-md:p-4">
        <h3 className="text-[15px] font-semibold mb-3.5">시스템 상태</h3>
        {[
          { label: '서버', status: 'UP' },
          { label: '데이터베이스', status: 'UP' },
          { label: 'Claude API', status: 'UP' },
          { label: '활성 파이프라인', status: '1개' },
        ].map(item => (
          <div key={item.label} className="flex items-center gap-2.5 py-1.5 text-sm max-md:text-[13px]">
            <span className="text-text-secondary min-w-[120px]">{item.label}</span>
            {item.status === 'UP' && <span className="w-2 h-2 bg-success rounded-full" />}
            <span>{item.status}</span>
          </div>
        ))}
      </div>
    </ModalOverlay>
  );
}
