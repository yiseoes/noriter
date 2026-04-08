import { useState, useRef } from 'react';
import type { ProjectStatus } from '../../types';

interface PreviewTabProps {
  projectId: string;
  projectStatus: ProjectStatus;
  onFeedback?: (text: string) => void;
  isGuest?: boolean;
  feedbackCount?: number;
  isAdmin?: boolean;
}

const FEEDBACK_LIMIT = 2;

export default function PreviewTab({ projectId, projectStatus, onFeedback, isGuest, feedbackCount = 0, isAdmin }: PreviewTabProps) {
  const [feedback, setFeedback] = useState('');
  const iframeRef = useRef<HTMLIFrameElement>(null);

  const isCompleted = projectStatus === 'COMPLETED';
  const isFailed = projectStatus === 'FAILED';
  const isInProgress = projectStatus === 'IN_PROGRESS' || projectStatus === 'REVISION';
  const previewUrl = `/api/projects/${projectId}/preview`;

  const feedbackRemaining = isAdmin ? Infinity : FEEDBACK_LIMIT - feedbackCount;
  const canFeedback = !isGuest && isCompleted && (isAdmin || feedbackRemaining > 0);

  const handleRefresh = () => {
    if (iframeRef.current) {
      iframeRef.current.src = previewUrl;
    }
  };

  const handleFullscreen = () => {
    iframeRef.current?.requestFullscreen?.();
  };

  return (
    <div>
      {isCompleted && (
        <div className="flex gap-2 mb-4 max-md:flex-wrap">
          <button
            onClick={handleRefresh}
            className="px-4 py-2 rounded-lg text-sm font-medium border border-border bg-bg-primary text-text-secondary cursor-pointer hover:bg-bg-tertiary"
          >
            🔄 새로고침
          </button>
          <button
            onClick={handleFullscreen}
            className="px-4 py-2 rounded-lg text-sm font-medium border border-border bg-bg-primary text-text-secondary cursor-pointer hover:bg-bg-tertiary"
          >
            ⛶ 전체화면
          </button>
          <a
            href={`/api/projects/${projectId}/download`}
            className="px-4 py-2 rounded-lg text-sm font-semibold bg-gradient-to-br from-brand to-[#a29bfe]
                       text-white no-underline cursor-pointer"
          >
            📥 다운로드 ZIP
          </a>
        </div>
      )}

      {isCompleted ? (
        <iframe
          ref={iframeRef}
          src={previewUrl}
          className="w-full h-[500px] border-2 border-border rounded-xl bg-[#0f3460] max-md:h-[280px]"
          title="게임 미리보기"
          sandbox="allow-scripts allow-same-origin"
        />
      ) : (
        <div className="w-full h-[300px] border-2 border-border rounded-xl bg-bg-secondary
                        flex flex-col items-center justify-center gap-3 max-md:h-[200px]">
          {isInProgress && (
            <>
              <div className="text-4xl animate-pulse">⏳</div>
              <div className="text-sm text-text-muted">게임을 만들고 있어요...</div>
              <div className="text-xs text-text-muted">완료되면 여기서 바로 플레이할 수 있어요!</div>
            </>
          )}
          {isFailed && (
            <>
              <div className="text-4xl">😢</div>
              <div className="text-sm text-danger">게임 생성에 실패했습니다.</div>
              <div className="text-xs text-text-muted">재시도하거나 다른 요구사항으로 다시 만들어보세요.</div>
            </>
          )}
          {projectStatus === 'CREATED' && (
            <>
              <div className="text-4xl">🎮</div>
              <div className="text-sm text-text-muted">게임 생성 대기 중...</div>
            </>
          )}
          {projectStatus === 'CANCELLED' && (
            <>
              <div className="text-4xl">🚫</div>
              <div className="text-sm text-text-muted">게임 만들기가 중단되었습니다.</div>
            </>
          )}
        </div>
      )}

      {isCompleted && (
        <div className="mt-5 p-5 bg-bg-secondary rounded-xl border border-border-light max-md:p-3.5">
          <div className="flex justify-between items-center mb-1.5">
            <label className="text-sm font-semibold text-text-secondary">수정 요청</label>
            {!isAdmin && !isGuest && (
              <span className={`text-xs ${feedbackRemaining <= 0 ? 'text-danger' : 'text-text-muted'}`}>
                {feedbackRemaining <= 0 ? '수정 횟수 소진' : `남은 수정 횟수: ${feedbackRemaining}/${FEEDBACK_LIMIT}`}
              </span>
            )}
          </div>

          {isGuest ? (
            <div className="text-sm text-text-muted py-4 text-center">
              수정 요청은 로그인 후 이용할 수 있습니다.
            </div>
          ) : (
            <>
              <textarea
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
                placeholder="수정할 내용을 입력하세요 (최소 5자)"
                disabled={!canFeedback}
                className="w-full min-h-[80px] bg-bg-primary border border-border rounded-[10px] px-3.5 py-2.5
                           text-sm resize-y outline-none focus:border-brand focus:ring-3 focus:ring-brand/10
                           disabled:opacity-50"
              />
              <button
                disabled={feedback.length < 5 || !canFeedback}
                onClick={() => { onFeedback?.(feedback); setFeedback(''); }}
                className="mt-3 w-full py-3 bg-gradient-to-br from-brand to-[#a29bfe] text-white
                           rounded-[10px] text-[15px] font-semibold cursor-pointer border-none
                           disabled:opacity-50 disabled:cursor-not-allowed"
              >
                수정 요청 보내기
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
}
