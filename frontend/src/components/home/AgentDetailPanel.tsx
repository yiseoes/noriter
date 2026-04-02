interface Stage {
  agent: string;
  color: string;
  detail: string;
  input: string;
  outputDesc: string;
  example: string;
}

interface AgentDetailPanelProps {
  stage: Stage;
}

export default function AgentDetailPanel({ stage }: AgentDetailPanelProps) {
  return (
    <div className="bg-bg-secondary border-2 border-border-light rounded-2xl p-5 px-6
                    flex gap-5 text-left max-w-[700px] mx-auto
                    max-md:flex-col max-md:gap-3"
         style={{ animation: 'fadeUp 0.4s ease both' }}>
      <div className="flex-1">
        <div className="text-sm font-bold mb-2" style={{ color: stage.color }}>
          {stage.agent}은 이런 일을 해요
        </div>
        <div className="text-xs text-text-secondary leading-relaxed">{stage.detail}</div>
        <div className="mt-3">
          <span className="inline-block px-2 py-0.5 rounded-md text-[10px] font-semibold mr-1"
                style={{ background: stage.color + '15', color: stage.color }}>입력</span>
          <span className="text-xs text-text-secondary">{stage.input}</span>
        </div>
        <div className="mt-1.5">
          <span className="inline-block px-2 py-0.5 rounded-md text-[10px] font-semibold mr-1"
                style={{ background: stage.color + '15', color: stage.color }}>출력</span>
          <span className="text-xs text-text-secondary">{stage.outputDesc}</span>
        </div>
      </div>
      <div className="flex-1 border-l-2 border-border-light pl-5
                      max-md:border-l-0 max-md:pl-0 max-md:border-t max-md:pt-3">
        <div className="text-sm font-bold mb-2">💬 실제 동작 예시</div>
        <div className="text-xs text-text-secondary leading-relaxed italic">{stage.example}</div>
      </div>
    </div>
  );
}
