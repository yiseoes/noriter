import { useState } from 'react';

interface SourceFile {
  name: string;
  content: string;
}

interface SourceTabProps {
  files: SourceFile[];
}

export default function SourceTab({ files }: SourceTabProps) {
  const [activeFile, setActiveFile] = useState(files[0]?.name || '');

  const currentFile = files.find(f => f.name === activeFile);
  const lines = currentFile?.content.split('\n') || [];

  return (
    <div className="flex gap-4 max-md:flex-col">
      {/* 파일 트리 */}
      <div className="w-[200px] shrink-0 bg-bg-secondary border border-border-light rounded-xl p-3
                      max-md:w-full max-md:flex max-md:gap-2 max-md:p-2">
        <div className="text-xs font-semibold text-text-muted mb-2 px-2.5 max-md:hidden">📁 game/</div>
        {files.map(f => (
          <button
            key={f.name}
            onClick={() => setActiveFile(f.name)}
            className={`w-full text-left px-2.5 py-1.5 rounded-md text-sm cursor-pointer flex items-center gap-1.5
                        max-md:whitespace-nowrap transition-colors
              ${activeFile === f.name
                ? 'bg-brand-bg text-brand font-semibold'
                : 'text-text-secondary hover:bg-bg-tertiary'
              }`}
          >
            📄 {f.name}
          </button>
        ))}
      </div>

      {/* 코드 뷰어 */}
      <div className="flex-1 bg-bg-secondary border border-border-light rounded-xl p-4
                      font-mono text-sm max-h-[500px] overflow-y-auto whitespace-pre leading-relaxed
                      [-webkit-overflow-scrolling:touch] max-md:max-h-[350px] max-md:text-[11px]">
        {lines.map((line, i) => (
          <div key={i}>
            <span className="inline-block w-[30px] text-right mr-4 text-text-muted select-none">{i + 1}</span>
            {line}
          </div>
        ))}
      </div>
    </div>
  );
}
