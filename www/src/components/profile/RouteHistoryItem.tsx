interface RouteHistoryItemProps {
  icon: string
  title: string
  date: string
  distance: string
  duration: string
  onViewDetails: () => void
}

export function RouteHistoryItem({
  icon,
  title,
  date,
  distance,
  duration,
  onViewDetails,
}: RouteHistoryItemProps) {
  return (
    <div className="bg-surface-container rounded-xl p-md md:p-lg flex flex-col md:flex-row md:items-center justify-between gap-md border border-transparent hover:bg-surface-container-high transition-colors shadow-sm shadow-black/20 group">
      <div className="flex items-center gap-md md:gap-lg">
        <div className="w-14 h-14 rounded-full bg-surface-container-highest flex items-center justify-center border-2 border-surface-container-highest">
          <span className="material-symbols-outlined text-on-surface-variant">{icon}</span>
        </div>
        <div className="flex flex-col gap-xs">
          <h3 className="font-title-lg text-title-lg text-primary">{title}</h3>
          <span className="font-body-md text-body-md text-on-surface-variant flex items-center gap-1">
            <span className="material-symbols-outlined text-[16px]">calendar_today</span>
            {date}
          </span>
        </div>
      </div>
      <div className="flex flex-col md:flex-row items-start md:items-center gap-md md:gap-xl mt-md md:mt-0">
        <div className="flex items-center gap-lg bg-surface py-2 px-4 rounded-full border border-surface-container-highest">
          <div className="flex flex-col items-center">
            <span className="font-label-sm text-label-sm text-on-surface-variant">Dystans</span>
            <span className="font-title-md text-title-md text-on-surface flex items-center gap-1">
              <span className="material-symbols-outlined text-[18px] text-primary-container">
                straighten
              </span>
              {distance}
            </span>
          </div>
          <div className="w-px h-8 bg-surface-container-highest" />
          <div className="flex flex-col items-center">
            <span className="font-label-sm text-label-sm text-on-surface-variant">
              Czas trwania
            </span>
            <span className="font-title-md text-title-md text-on-surface flex items-center gap-1">
              <span className="material-symbols-outlined text-[18px] text-primary-container">
                timer
              </span>
              {duration}
            </span>
          </div>
        </div>
        <button
          onClick={onViewDetails}
          className="w-full md:w-auto px-6 py-3 rounded-full bg-primary-container text-on-primary-container font-label-lg text-label-lg hover:bg-primary transition-colors"
        >
          Zobacz szczegóły
        </button>
      </div>
    </div>
  )
}
