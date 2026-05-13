interface StatsCardProps {
  icon: string
  label: string
  value: string
  valueClassName?: string
}

export function StatsCard({ icon, label, value, valueClassName = 'text-white' }: StatsCardProps) {
  return (
    <div className="bg-cardDark rounded-2xl p-6 flex flex-col justify-between shadow-lg shadow-black/20 border border-gray-800/50">
      <div className="flex items-center space-x-2 text-primaryAccent mb-4">
        <i className={`${icon} text-sm`} />
        <span className="text-xs font-semibold tracking-wider uppercase text-textMuted">
          {label}
        </span>
      </div>
      <div className={`text-3xl font-bold ${valueClassName}`}>{value}</div>
    </div>
  )
}
