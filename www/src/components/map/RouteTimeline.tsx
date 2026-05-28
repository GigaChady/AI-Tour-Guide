export interface TimelinePoint {
  id: string
  title: string
}

interface RouteTimelineProps {
  points: TimelinePoint[]
}

export function RouteTimeline({ points }: RouteTimelineProps) {
  if (!points || points.length === 0) return null

  return (
    <div className="flex flex-col gap-1 relative pl-8 mt-4">
      <div className="absolute left-2.75 top-4 bottom-4 w-0.5 bg-surface-variant" />

      {points.map((point, index) => {
        const isStart = index === 0
        const isEnd = index === points.length - 1
        const isMiddle = !isStart && !isEnd
        let dotContent = <div className="w-2 h-2 rounded-full bg-primary" />
        let dotClass = 'border-primary'
        let label = 'Przystanek'

        if (isEnd) {
          dotContent = (
            <span
              className="material-symbols-outlined text-[12px] text-tertiary"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              location_on
            </span>
          )
          dotClass = 'border-tertiary'
          label = 'Koniec'
        }

        if (isStart) {
          label = 'Start'
        }

        return (
          <div
            key={point.id}
            className={`flex items-center gap-4 relative ${isMiddle ? 'py-1' : 'mt-2'}`}
          >
            <div
              className={`absolute -left-8 w-6 h-6 rounded-full bg-surface-container-high border-2 ${dotClass} flex items-center justify-center z-10`}
            >
              {dotContent}
            </div>
            <div>
              {!isMiddle && (
                <p className="font-label-sm text-on-surface-variant uppercase text-label-sm">
                  {label}
                </p>
              )}
              <p className="font-title-md text-on-surface">{point.title}</p>
            </div>
          </div>
        )
      })}
    </div>
  )
}
