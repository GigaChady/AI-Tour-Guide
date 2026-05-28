interface SummaryStatCardProps {
  icon: string
  label: string
  value: string
  unit: string
  colorClass?: string
}

export function SummaryStatCard({
  icon,
  label,
  value,
  unit,
  colorClass = 'text-primary',
}: SummaryStatCardProps) {
  return (
    <div className="bg-surface-container rounded-lg p-4 flex flex-col justify-between hover:bg-surface-container-high transition-colors">
      <div className={`flex items-center gap-2 ${colorClass} mb-2`}>
        <span className="material-symbols-outlined text-sm">{icon}</span>
        <span className="font-label-lg text-sm">{label}</span>
      </div>
      <div className="flex items-baseline gap-1">
        <span className="text-3xl font-bold text-on-surface">{value}</span>
        <span className="font-body-md text-on-surface-variant">{unit}</span>
      </div>
    </div>
  )
}
