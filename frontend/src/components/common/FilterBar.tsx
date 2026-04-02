interface FilterBarProps {
  filters: string[];
  active: string;
  onChange: (filter: string) => void;
}

export default function FilterBar({ filters, active, onChange }: FilterBarProps) {
  return (
    <div className="flex gap-1.5 overflow-x-auto pb-1 -webkit-overflow-scrolling-touch">
      {filters.map(f => (
        <button
          key={f}
          onClick={() => onChange(f)}
          className={`shrink-0 whitespace-nowrap px-3.5 py-1.5 rounded-md text-sm border cursor-pointer transition-colors
            ${active === f
              ? 'bg-coral border-coral text-white'
              : 'bg-bg-primary border-border-light text-text-secondary hover:bg-bg-tertiary'
            }`}
        >
          {f}
        </button>
      ))}
    </div>
  );
}
