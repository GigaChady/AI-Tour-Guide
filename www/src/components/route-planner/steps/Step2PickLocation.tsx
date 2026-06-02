import type { ReceivedPoi } from '../useRoutePlannerSession'

interface Step2PickLocationProps {
  pois: ReceivedPoi[]
  isLoading: boolean
  selectedPoi: ReceivedPoi | null
  onSelect: (poi: ReceivedPoi) => void
  selectedMapPoint?: { lat: number; lng: number } | null
}

function makeCustomPoi(point: { lat: number; lng: number }): ReceivedPoi {
  return {
    name: 'Wybrany punkt',
    lat: point.lat,
    lng: point.lng,
    photos: [],
    desc: null,
    narration_id: `custom-${point.lat}-${point.lng}`,
  }
}

export function Step2PickLocation({ pois, isLoading, selectedPoi, onSelect, selectedMapPoint }: Step2PickLocationProps) {
  const customPoi = selectedMapPoint ? makeCustomPoi(selectedMapPoint) : null
  const hasContent = customPoi !== null || pois.length > 0

  if (!hasContent && !isLoading) {
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

  const renderItem = (poi: ReceivedPoi, isCustom = false) => {
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
          {isSelected ? 'check_circle' : isCustom ? 'pin_drop' : 'place'}
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
  }

  const isCustomSelected = customPoi
    ? selectedPoi?.narration_id === customPoi.narration_id && selectedPoi?.name === customPoi.name
    : false

  return (
    <div className="flex flex-col gap-3 flex-1 min-h-0 overflow-hidden">
      {customPoi && (
        <div
          onClick={() => onSelect(customPoi)}
          className={`flex items-center gap-2.5 p-2.5 rounded-xl cursor-pointer transition-colors ${
            isCustomSelected
              ? 'bg-primary/20 border border-primary/40'
              : 'bg-surface-container hover:bg-surface-container-high'
          }`}
        >
          <span className={`material-symbols-outlined text-base ${isCustomSelected ? 'text-primary' : 'text-on-surface-variant'}`}>
            {isCustomSelected ? 'check_circle' : 'pin_drop'}
          </span>
          <span className="font-body-md text-on-surface">Wybrany punkt</span>
        </div>
      )}

      {(isLoading || pois.length > 0) && (
        <div className="flex flex-col gap-2 flex-1 min-h-0 overflow-hidden">
          <p className="font-label-sm text-on-surface-variant uppercase tracking-wider flex-shrink-0">
            Znalezione miejsca
          </p>
          <ul className="flex flex-col gap-1.5 overflow-y-auto pr-1 flex-1 min-h-0">
            {isLoading && (
              <li className="flex items-center gap-2.5 p-2.5 rounded-xl bg-surface-container">
                <span className="material-symbols-outlined text-base text-primary animate-spin">progress_activity</span>
                <span className="font-body-sm text-on-surface-variant">Wyszukiwanie atrakcji w pobliżu…</span>
              </li>
            )}
            {pois.map((poi) => renderItem(poi))}
          </ul>
        </div>
      )}
    </div>
  )
}
