import { Fragment } from 'react'

interface SummaryStat {
  icon: string
  label: string
  value: string
}

interface NavigatorSummaryCardProps {
  avatarSrc: string
  name: string
  role: string
  explorationCount: number
  stats: SummaryStat[]
}

export function NavigatorSummaryCard({
  avatarSrc,
  name,
  role,
  explorationCount,
  stats,
}: NavigatorSummaryCardProps) {
  return (
    <div className="bg-surface-container rounded-xl p-md md:p-lg mb-lg flex flex-col md:flex-row items-center justify-between gap-md border border-primary/20">
      <div className="flex items-center gap-md md:gap-lg">
        <img
          alt="Awatar podróżnika"
          className="w-20 h-20 rounded-full object-cover border-2 border-primary"
          src={avatarSrc}
        />
        <div className="flex flex-col gap-xs">
          <span className="font-label-sm text-label-sm text-primary uppercase tracking-wider">
            {role}
          </span>
          <h3 className="font-headline-md text-headline-md text-on-surface">
            {name}
          </h3>
          <span className="font-body-md text-body-md text-on-surface-variant">
            Łącznie eksploracji: {explorationCount}
          </span>
        </div>
      </div>
      <div className="flex items-center gap-lg bg-surface py-3 px-6 rounded-xl border border-surface-container-highest">
        {stats.map((stat, i) => (
          <Fragment key={stat.label}>
            {i > 0 && <div className="w-px h-10 bg-surface-container-highest" />}
            <div className="flex flex-col items-center">
              <span className="font-label-sm text-label-sm text-on-surface-variant">
                {stat.label}
              </span>
              <span className="font-title-lg text-title-lg text-primary flex items-center gap-1">
                <span className="material-symbols-outlined text-[20px]">{stat.icon}</span>
                {stat.value}
              </span>
            </div>
          </Fragment>
        ))}
      </div>
    </div>
  )
}
