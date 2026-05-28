interface SaveButtonProps {
  label: string
  onClick?: () => void
  className?: string
}

export function SaveButton({ label, onClick, className = '' }: SaveButtonProps) {
  return (
    <div className={`pt-6 border-t border-outline-variant/20 flex justify-end ${className}`}>
      <button
        onClick={onClick}
        className="px-8 py-3 rounded-full bg-primary text-on-primary font-title-md hover:bg-primary-fixed transition-colors shadow-md"
      >
        {label}
      </button>
    </div>
  )
}
