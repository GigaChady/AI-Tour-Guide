type MetricVariant = 'primary' | 'tertiary'

interface AdminMetricCardProps {
  label: string
  icon: string
  value: string
  unit?: string
  percentage: number
  variant: MetricVariant
}

const variantStyles: Record<
  MetricVariant,
  { gradient: string; icon: string; value: string; bar: string }
> = {
  primary: {
    gradient: 'from-primary-container/10',
    icon: 'text-primary',
    value: 'text-primary-fixed-dim',
    bar: 'bg-primary',
  },
  tertiary: {
    gradient: 'from-tertiary-container/10',
    icon: 'text-tertiary',
    value: 'text-tertiary-fixed-dim',
    bar: 'bg-tertiary',
  },
}

export function AdminMetricCard({
  label,
  icon,
  value,
  unit,
  percentage,
  variant,
}: AdminMetricCardProps) {
  const s = variantStyles[variant]

  return (
    <div className="bg-surface-container rounded-xl p-md border border-white/5 relative overflow-hidden group">
      <div
        className={`absolute inset-0 bg-gradient-to-br ${s.gradient} to-transparent opacity-50 group-hover:opacity-100 transition-opacity duration-500`}
      />
      <div className="flex items-center justify-between mb-4 relative z-10">
        <span className="font-label-lg text-label-lg text-on-surface-variant">{label}</span>
        <span className={`material-symbols-outlined ${s.icon}`}>{icon}</span>
      </div>
      <div className="relative z-10">
        <div className={`font-display-lg text-display-lg ${s.value}`}>
          {value}
          {unit && <span className="text-title-md ml-1 opacity-70">{unit}</span>}
        </div>
        <div className="w-full bg-surface-container-highest rounded-full h-2 mt-2">
          <div className={`${s.bar} h-2 rounded-full`} style={{ width: `${percentage}%` }} />
        </div>
      </div>
    </div>
  )
}
