import type { ReceivedPoi } from '../useRoutePlannerSession'

interface Step2PickLocationProps {
  pois: ReceivedPoi[]
  isLoading: boolean
  selectedPoi: ReceivedPoi | null
  onSelect: (poi: ReceivedPoi) => void
}

export function Step2PickLocation({ pois, isLoading, selectedPoi, onSelect }: Step2PickLocationProps) {
  if (isLoading) {
    return (
      <div className="flex flex-col items-center gap-3 py-2 text-center">
        <span className="material-symbols-outlined text-3xl text-primary animate-spin">
          progress_activity
        </span>
        <p className="font-body-md text-on-surface-variant">
          Wyszukiwanie atrakcji w pobliżu…
        </p>
      </div>
    )
  }

  if (pois.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 py-2 text-center">
        <span className="material-symbols-outlined text-3xl text-primary animate-pulse">
          location_searching
        </span>
        <p className="font-body-md text-on-surface-variant">
          Kliknij punkt na mapie, aby znaleźć pobliskie atrakcje
        </p>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <p className="font-label-sm text-on-surface-variant uppercase tracking-wider">
        Znalezione miejsca
      </p>
      <ul className="flex flex-col gap-1.5 max-h-48 overflow-y-auto pr-1">
        {pois.map((poi) => {
          const isSelected = selectedPoi?.narration_id === poi.narration_id && selectedPoi?.name === poi.name
          return (
            <li
              key={poi.narration_id + poi.name}
              onClick={() => onSelect(poi)}
              className={`flex items-start gap-2.5 p-2.5 rounded-xl cursor-pointer transition-colors ${
                isSelected
                  ? 'bg-primary/20 border border-primary/40'
                  : 'bg-surface-container hover:bg-surface-container-high'
              }`}
            >
              <span className={`material-symbols-outlined text-base mt-0.5 ${isSelected ? 'text-primary' : 'text-on-surface-variant'}`}>
                {isSelected ? 'check_circle' : 'place'}
              </span>
              <div className="flex flex-col min-w-0">
                <span className="font-body-md text-on-surface truncate">{poi.name}</span>
                {poi.desc && poi.desc !== poi.name && (
                  <span className="font-body-sm text-on-surface-variant line-clamp-2">
                    {poi.desc}
                  </span>
                )}
              </div>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
