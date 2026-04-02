interface GenreTagProps {
  label: string;
  selected?: boolean;
  onClick?: () => void;
}

export default function GenreTag({ label, selected = false, onClick }: GenreTagProps) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-1.5 rounded-full text-sm font-medium border cursor-pointer transition-colors
        ${selected
          ? 'bg-brand border-brand text-white'
          : 'bg-transparent border-border text-text-secondary hover:border-brand hover:text-brand'
        }`}
    >
      {label}
    </button>
  );
}
