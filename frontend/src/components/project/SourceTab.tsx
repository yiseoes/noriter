import { useState, useRef, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { saveGameSource } from '../../api/gameApi';

interface SourceFile {
  name: string;
  content: string;
}

interface SourceTabProps {
  files: SourceFile[];
  projectId: string;
}

export default function SourceTab({ files, projectId }: SourceTabProps) {
  const [activeFile, setActiveFile] = useState(files[0]?.name || '');
  const [editMode, setEditMode] = useState(false);
  const [editContent, setEditContent] = useState('');
  const [savedFile, setSavedFile] = useState<string | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [matchIndex, setMatchIndex] = useState(0);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const viewerRef = useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();

  const currentFile = files.find(f => f.name === activeFile);
  const lines = currentFile?.content.split('\n') || [];

  // 검색 매칭 줄 인덱스 목록
  const matchedLines = searchQuery.trim()
    ? lines.reduce<number[]>((acc, line, i) => {
        if (line.toLowerCase().includes(searchQuery.toLowerCase())) acc.push(i);
        return acc;
      }, [])
    : [];

  const currentMatchLine = matchedLines[matchIndex] ?? -1;

  // 저장 성공 시 warnings 반영
  useEffect(() => {
    if (saveMutation.isSuccess) {
      setWarnings(saveMutation.data?.warnings ?? []);
    }
  }, [saveMutation.isSuccess, saveMutation.data]);

  // 매칭 줄로 자동 스크롤
  useEffect(() => {
    if (currentMatchLine < 0 || !viewerRef.current) return;
    const lineEl = viewerRef.current.querySelector(`[data-line="${currentMatchLine}"]`);
    lineEl?.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }, [currentMatchLine]);

  // 검색창 열릴 때 포커스
  useEffect(() => {
    if (searchOpen) searchInputRef.current?.focus();
  }, [searchOpen]);

  // 파일 전환 시 검색 초기화
  useEffect(() => {
    setMatchIndex(0);
  }, [searchQuery, activeFile]);

  const saveMutation = useMutation({
    mutationFn: ({ path, content }: { path: string; content: string }) =>
      saveGameSource(projectId, path, content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['gameFiles', projectId] });
      setSavedFile(activeFile);
      setEditMode(false);
      setTimeout(() => setSavedFile(null), 2000);
    },
  });

  const handleEdit = () => {
    setEditContent(currentFile?.content || '');
    setEditMode(true);
    saveMutation.reset();
    setWarnings([]);
    setSearchOpen(false);
    setSearchQuery('');
  };

  const handleCancel = () => {
    setEditMode(false);
    setEditContent('');
    saveMutation.reset();
    setWarnings([]);
  };

  const handleSave = () => {
    saveMutation.mutate({ path: activeFile, content: editContent });
  };

  const handleFileChange = (name: string) => {
    if (editMode) {
      if (!confirm('편집 중인 내용이 있습니다. 파일을 전환하면 변경사항이 사라집니다.')) return;
      setEditMode(false);
    }
    setActiveFile(name);
    saveMutation.reset();
    setWarnings([]);
    setSearchQuery('');
    setMatchIndex(0);
  };

  const handleSearchKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (matchedLines.length === 0) return;
      setMatchIndex(prev => (prev + 1) % matchedLines.length);
    }
    if (e.key === 'Escape') {
      setSearchOpen(false);
      setSearchQuery('');
    }
  };

  const goPrev = () => {
    if (matchedLines.length === 0) return;
    setMatchIndex(prev => (prev - 1 + matchedLines.length) % matchedLines.length);
  };

  const goNext = () => {
    if (matchedLines.length === 0) return;
    setMatchIndex(prev => (prev + 1) % matchedLines.length);
  };

  // 텍스트에서 검색어 하이라이트
  const highlightText = (text: string) => {
    if (!searchQuery.trim()) return <>{text}</>;
    const parts = text.split(new RegExp(`(${searchQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi'));
    return (
      <>
        {parts.map((part, i) =>
          part.toLowerCase() === searchQuery.toLowerCase()
            ? <mark key={i} className="bg-yellow-300 text-black rounded-sm px-0.5">{part}</mark>
            : part
        )}
      </>
    );
  };

  return (
    <div className="flex gap-4 max-md:flex-col">
      {/* 파일 트리 */}
      <div className="w-[200px] shrink-0 bg-bg-secondary border border-border-light rounded-xl p-3
                      max-md:w-full max-md:flex max-md:gap-2 max-md:p-2">
        <div className="text-xs font-semibold text-text-muted mb-2 px-2.5 max-md:hidden">📁 game/</div>
        {files.map(f => (
          <button
            key={f.name}
            onClick={() => handleFileChange(f.name)}
            className={`w-full text-left px-2.5 py-1.5 rounded-md text-sm cursor-pointer flex items-center gap-1.5
                        max-md:whitespace-nowrap transition-colors
              ${activeFile === f.name
                ? 'bg-brand-bg text-brand font-semibold'
                : 'text-text-secondary hover:bg-bg-tertiary'
              }`}
          >
            📄 {f.name}
            {savedFile === f.name && <span className="ml-auto text-xs text-green-500">✓</span>}
          </button>
        ))}
      </div>

      {/* 코드 뷰어 / 에디터 */}
      <div className="flex-1 flex flex-col gap-2">
        {/* 툴바 */}
        <div className="flex items-center justify-between gap-2 flex-wrap">
          <span className="text-sm text-text-muted font-mono truncate">{activeFile}</span>
          <div className="flex gap-2 shrink-0">
            {!editMode && (
              <button
                onClick={() => { setSearchOpen(v => !v); setSearchQuery(''); setMatchIndex(0); }}
                className={`px-3 py-1.5 text-sm rounded-lg border transition-colors
                  ${searchOpen
                    ? 'bg-bg-tertiary border-brand text-brand'
                    : 'border-border-light text-text-secondary hover:bg-bg-tertiary'
                  }`}
              >
                🔍 찾기
              </button>
            )}
            {!editMode ? (
              <button
                onClick={handleEdit}
                className="px-3 py-1.5 text-sm bg-brand text-white rounded-lg hover:bg-brand/80 transition-colors"
              >
                ✏️ 편집
              </button>
            ) : (
              <>
                <button
                  onClick={handleCancel}
                  className="px-3 py-1.5 text-sm border border-border-light text-text-secondary rounded-lg hover:bg-bg-tertiary transition-colors"
                >
                  취소
                </button>
                <button
                  onClick={handleSave}
                  disabled={saveMutation.isPending}
                  className="px-3 py-1.5 text-sm bg-green-500 text-white rounded-lg hover:bg-green-600 disabled:opacity-50 transition-colors"
                >
                  {saveMutation.isPending ? '저장 중...' : '💾 저장 & 반영'}
                </button>
              </>
            )}
          </div>
        </div>

        {/* 검색 바 */}
        {searchOpen && !editMode && (
          <div className="flex items-center gap-2 bg-bg-secondary border border-border-light rounded-xl px-3 py-2">
            <input
              ref={searchInputRef}
              value={searchQuery}
              onChange={e => { setSearchQuery(e.target.value); setMatchIndex(0); }}
              onKeyDown={handleSearchKeyDown}
              placeholder="검색어 입력... (Enter: 다음)"
              className="flex-1 bg-transparent text-sm font-mono text-text-primary outline-none placeholder:text-text-muted"
            />
            {searchQuery && (
              <span className="text-xs text-text-muted shrink-0">
                {matchedLines.length > 0 ? `${matchIndex + 1} / ${matchedLines.length}` : '없음'}
              </span>
            )}
            <button onClick={goPrev} disabled={matchedLines.length === 0}
              className="text-text-muted hover:text-text-primary disabled:opacity-30 px-1">▲</button>
            <button onClick={goNext} disabled={matchedLines.length === 0}
              className="text-text-muted hover:text-text-primary disabled:opacity-30 px-1">▼</button>
            <button onClick={() => { setSearchOpen(false); setSearchQuery(''); }}
              className="text-text-muted hover:text-text-primary px-1">✕</button>
          </div>
        )}

        {/* 에디터 / 뷰어 */}
        {editMode ? (
          <textarea
            value={editContent}
            onChange={e => setEditContent(e.target.value)}
            className="w-full h-[500px] max-md:h-[350px] bg-bg-secondary border border-brand rounded-xl p-4
                       font-mono text-sm leading-relaxed resize-none outline-none overflow-y-auto
                       text-text-primary whitespace-pre-wrap break-all max-md:text-[11px]"
            spellCheck={false}
          />
        ) : (
          <div
            ref={viewerRef}
            className="w-full h-[500px] max-md:h-[350px] bg-bg-secondary border border-border-light rounded-xl p-4
                       font-mono text-sm overflow-y-auto whitespace-pre-wrap break-all leading-relaxed
                       [-webkit-overflow-scrolling:touch] max-md:text-[11px]"
          >
            {lines.map((line, i) => (
              <div
                key={i}
                data-line={i}
                className={`transition-colors rounded-sm
                  ${matchedLines.includes(i)
                    ? i === currentMatchLine
                      ? 'bg-yellow-200/60'
                      : 'bg-yellow-100/30'
                    : ''
                  }`}
              >
                <span className="inline-block w-[30px] text-right mr-4 text-text-muted select-none">{i + 1}</span>
                {searchQuery.trim() ? highlightText(line) : line}
              </div>
            ))}
          </div>
        )}

        {/* 커플링 경고 */}
        {warnings.length > 0 && (
          <div className="rounded-xl border border-yellow-400/50 bg-yellow-50 dark:bg-yellow-900/20 p-3 flex flex-col gap-1.5">
            <div className="flex items-center gap-1.5 text-sm font-semibold text-yellow-700 dark:text-yellow-400">
              ⚠️ 커플링 경고 — 다른 파일과 불일치가 감지되었습니다
            </div>
            <ul className="flex flex-col gap-1">
              {warnings.map((w, i) => (
                <li key={i} className="text-xs text-yellow-700 dark:text-yellow-300 font-mono leading-relaxed">
                  {w}
                </li>
              ))}
            </ul>
            <p className="text-xs text-yellow-600 dark:text-yellow-400 mt-0.5">
              위 항목을 확인하고 관련 파일도 함께 수정하세요.
            </p>
          </div>
        )}

        {saveMutation.isError && (
          <p className="text-sm text-red-500">저장 실패. 다시 시도해주세요.</p>
        )}
      </div>
    </div>
  );
}
