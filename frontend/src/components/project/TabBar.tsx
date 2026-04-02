interface TabBarProps {
  tabs: { key: string; label: string }[];
  active: string;
  onChange: (key: string) => void;
}

export default function TabBar({ tabs, active, onChange }: TabBarProps) {
  return (
    <div className="flex gap-0.5 bg-bg-tertiary rounded-[10px] p-[3px] my-5
                    max-md:flex-nowrap max-md:overflow-x-auto max-md:[-webkit-overflow-scrolling:touch]">
      {tabs.map(tab => (
        <button
          key={tab.key}
          onClick={() => onChange(tab.key)}
          className={`flex-1 py-2.5 text-center rounded-lg text-sm font-medium cursor-pointer transition-colors
                      max-md:flex-none max-md:px-3.5 max-md:py-2 max-md:text-xs max-md:whitespace-nowrap
            ${active === tab.key
              ? 'bg-bg-primary text-brand shadow-sm'
              : 'text-text-secondary hover:text-text-primary'
            }`}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
