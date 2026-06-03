import type { SavedPoi } from './useRoutePlannerSession'

interface RouteSummaryCardProps {
  pois: SavedPoi[]
  distanceM: number | null
  onRemove: (index: number) => void
}

function formatDistance(metres: number): string {
  return metres >= 1000
    ? `${(metres / 1000).toFixed(1)} km`
    : `${metres} m`
}

export function RouteSummaryCard({ pois, distanceM, onRemove }: RouteSummaryCardProps) {
  return (
    <div className="bg-surface-container-highest/95 backdrop-blur-2xl rounded-2xl border border-primary/20 p-3 flex flex-col gap-3 pointer-events-auto flex-1 min-h-0 overflow-hidden">
      <div className="flex items-center gap-2">
        <span className="material-symbols-outlined text-primary text-xl">map</span>
        <span className="text-xs font-semibold uppercase tracking-wider text-on-surface">
          Wybrane miejsca
        </span>
        <div className="ml-auto flex items-center gap-2">
          {distanceM !== null && (
            <span className="text-xs text-on-surface-variant flex items-center gap-1">
              <span className="material-symbols-outlined text-sm">straighten</span>
              {formatDistance(distanceM)}
            </span>
          )}
          <span className="text-xs font-semibold text-primary bg-primary/15 rounded-full px-2 py-0.5">
            {pois.length}
          </span>
        </div>
      </div>

      <div className="h-px bg-outline-variant/30" />

      {pois.length === 0 ? (
        <p className="font-body-md text-on-surface-variant text-center py-1">
          Brak wybranych miejsc
        </p>
      ) : (
        <ol className="flex flex-col gap-1.5 overflow-y-auto pr-3 flex-1 min-h-0">
          {pois.map((poi, index) => (
            <li key={`${poi.name}-${poi.lat}-${poi.lng}`} className="flex items-start gap-2.5">
              <span className="flex-shrink-0 w-5 h-5 rounded-full bg-primary/15 text-primary text-xs font-semibold flex items-center justify-center mt-0.5">
                {index + 1}
              </span>
              <div className="flex flex-col min-w-0 flex-1">
                <span className="font-body-md text-on-surface truncate">{poi.name}</span>
                {poi.description && poi.description !== poi.name && (
                  <span className="font-body-sm text-on-surface-variant line-clamp-1">{poi.description}</span>
                )}
              </div>
              <button
                onClick={() => onRemove(index)}
                className="flex-shrink-0 text-on-surface-variant hover:text-error transition-colors"
              >
                <span className="material-symbols-outlined text-base">close</span>
              </button>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}
