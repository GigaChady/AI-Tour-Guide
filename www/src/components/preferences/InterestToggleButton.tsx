interface InterestToggleButtonProps {
  label: string
  icon: string
  isActive: boolean
  onClick: () => void
}

export function InterestToggleButton({ label, icon, isActive, onClick }: InterestToggleButtonProps) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2 rounded-full font-label-lg flex items-center gap-2 transition-all ${
        isActive
          ? 'bg-tertiary-container text-on-tertiary-container border border-tertiary-container shadow-[0_0_10px_rgba(239,184,200,0.4)]'
          : 'bg-surface-container text-on-surface border border-outline-variant/50 hover:bg-surface-container-highest'
      }`}
    >
      {isActive ? (
        <span className="material-symbols-outlined text-sm">check</span>
      ) : (
        <span className="material-symbols-outlined text-sm">{icon}</span>
      )}
      {label}
    </button>
  )
}
