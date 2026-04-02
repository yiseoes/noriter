const agents = [
  { name: '기획팀', desc: '게임 기획서 작성', color: 'text-agent-planning', icon: '📋' },
  { name: 'CTO', desc: '기술 아키텍처 결정', color: 'text-agent-cto', icon: '👔' },
  { name: '디자인팀', desc: 'UI/UX 디자인', color: 'text-agent-design', icon: '🎨' },
  { name: '프론트팀', desc: '화면·인터랙션 구현', color: 'text-agent-frontend', icon: '💻' },
  { name: '백엔드팀', desc: '게임 로직 구현', color: 'text-agent-backend', icon: '⚙️' },
  { name: 'QA팀', desc: '테스트·품질 보증', color: 'text-agent-qa', icon: '🔍' },
];

export default function AgentCards() {
  return (
    <section className="min-h-screen flex flex-col justify-center items-center
                        py-[60px] px-8 bg-bg-secondary snap-start
                        max-md:py-10 max-md:px-4">
      <div className="max-w-[1000px] w-full text-center">
        <div className="text-sm font-semibold text-brand tracking-widest uppercase mb-2">
          Meet the Crew
        </div>
        <h2 className="text-[28px] font-bold mb-10 max-md:text-xl max-md:mb-6">
          6명의 AI 친구들을 소개할게요 👋
        </h2>

        <div className="grid grid-cols-6 gap-3.5 max-md:grid-cols-3 max-md:gap-2 max-[380px]:grid-cols-2">
          {agents.map((agent, i) => (
            <div
              key={agent.name}
              className="bg-bg-primary border-2 border-border-light rounded-[18px] py-6 px-3 text-center
                         cursor-default transition-all duration-400 ease-[cubic-bezier(0.34,1.56,0.64,1)]
                         hover:!rotate-0 hover:-translate-y-2 hover:scale-105 hover:shadow-lg hover:border-brand-light
                         max-md:py-3.5 max-md:px-2"
              style={{ transform: `rotate(${i % 2 === 0 ? '-1.5deg' : '1.5deg'})` }}
            >
              <div className="text-[42px] mb-2.5 max-md:text-[36px]">{agent.icon}</div>
              <div className={`text-sm font-semibold mb-1 max-md:text-[11px] ${agent.color}`}>{agent.name}</div>
              <div className="text-[11px] text-text-muted max-md:text-[9px]">{agent.desc}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
