interface EmptyCardProps {
  onClick: () => void;
}

export default function EmptyCard({ onClick }: EmptyCardProps) {
  return (
    <div
      onClick={onClick}
      className="border-2 border-dashed border-border rounded-2xl flex flex-col items-center justify-center
                 min-h-[320px] cursor-pointer hover:border-brand hover:bg-brand-bg transition-colors"
    >
      <div className="text-[40px] mb-3 text-text-muted">✨</div>
      <div className="text-[15px] font-semibold text-text-secondary">나만의 게임 만들기</div>
      <div className="text-sm text-text-muted mt-1">AI에게 원하는 게임을 설명해보세요</div>
    </div>
  );
}
