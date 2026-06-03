interface StepDotsProps {
  current: number
  total: number
}

export function StepDots({ current, total }: StepDotsProps) {
  return (
    <div className="flex items-center gap-1.5">
      {Array.from({ length: total }).map((_, i) => (
        <span
          key={i}
          className={`rounded-full transition-all duration-300 ${
            i === current
              ? 'w-4 h-1.5 bg-primary'
              : i < current
                ? 'w-1.5 h-1.5 bg-primary/50'
                : 'w-1.5 h-1.5 bg-outline-variant'
          }`}
        />
      ))}
    </div>
  )
}
