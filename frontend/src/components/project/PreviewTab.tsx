import { useState } from 'react';

interface PreviewTabProps {
  projectId: string;
  onFeedback?: (text: string) => void;
}

export default function PreviewTab({ projectId, onFeedback }: PreviewTabProps) {
  const [feedback, setFeedback] = useState('');

  return (
    <div>
      <div className="flex gap-2 mb-4 max-md:flex-wrap">
        <button className="px-4 py-2 rounded-lg text-sm font-medium border border-border bg-bg-primary text-text-secondary cursor-pointer hover:bg-bg-tertiary">
          🔄 새로고침
        </button>
        <button className="px-4 py-2 rounded-lg text-sm font-medium border border-border bg-bg-primary text-text-secondary cursor-pointer hover:bg-bg-tertiary">
          ⛶ 전체화면
        </button>
        <a
          href={`/api/projects/${projectId}/game/download`}
          className="px-4 py-2 rounded-lg text-sm font-semibold bg-gradient-to-br from-brand to-[#a29bfe]
                     text-white no-underline cursor-pointer"
        >
          📥 다운로드 ZIP
        </a>
      </div>

      {/* 미리보기 프레임 */}
      <div className="w-full h-[500px] border-2 border-border rounded-xl bg-[#0f3460]
                      flex items-center justify-center text-white text-5xl max-md:h-[280px]">
        🎮
      </div>

      {/* 수정 요청 */}
      <div className="mt-5 p-5 bg-bg-secondary rounded-xl border border-border-light max-md:p-3.5">
        <label className="block text-sm font-semibold text-text-secondary mb-1.5">수정 요청</label>
        <textarea
          value={feedback}
          onChange={(e) => setFeedback(e.target.value)}
          placeholder="수정할 내용을 입력하세요 (최소 5자)"
          className="w-full min-h-[80px] bg-bg-primary border border-border rounded-[10px] px-3.5 py-2.5
                     text-sm resize-y outline-none focus:border-brand focus:ring-3 focus:ring-brand/10"
        />
        <button
          disabled={feedback.length < 5}
          onClick={() => { onFeedback?.(feedback); setFeedback(''); }}
          className="mt-3 w-full py-3 bg-gradient-to-br from-brand to-[#a29bfe] text-white
                     rounded-[10px] text-[15px] font-semibold cursor-pointer border-none
                     disabled:opacity-50 disabled:cursor-not-allowed"
        >
          수정 요청 보내기
        </button>
      </div>
    </div>
  );
}
