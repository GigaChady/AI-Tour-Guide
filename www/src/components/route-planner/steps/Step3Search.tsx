import { useEffect, useRef, useState } from 'react'
import api from '@/network/axios'
import type { ReceivedPoi } from '../useRoutePlannerSession'

interface PlaceResult {
  name: string
  lat: number
  lng: number
  description: string | null
}

interface Step3SearchProps {
  onConfirm: (poi: ReceivedPoi) => void
}

export function Step3Search({ onConfirm }: Step3SearchProps) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<PlaceResult[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [selected, setSelected] = useState<PlaceResult | null>(null)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    setSelected(null)
    if (!query.trim()) {
      setResults([])
      return
    }
    debounceRef.current = setTimeout(async () => {
      setIsLoading(true)
      try {
        const res = await api.get<PlaceResult[]>('/search', { params: { q: query } })
        setResults(res.data)
      } catch {
        setResults([])
      } finally {
        setIsLoading(false)
      }
    }, 400)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [query])

  const handleConfirm = () => {
    if (!selected) return
    onConfirm({
      name: selected.name,
      lat: selected.lat,
      lng: selected.lng,
      photos: [],
      desc: selected.description,
      narration_id: `search-${selected.lat}-${selected.lng}`,
    })
  }

  return (
    <div className="flex flex-col flex-1 min-h-0 gap-3">
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Szukaj miejsc…"
        autoFocus
        className="w-full bg-surface-container px-3 py-2.5 rounded-xl border border-outline-variant/40 text-on-surface font-body-md placeholder:text-on-surface-variant/40 focus:outline-none focus:border-primary/60 transition-colors"
      />
      <div className="flex-1 min-h-0 overflow-y-auto">
        {isLoading && (
          <div className="flex items-center gap-2 py-4">
            <span className="material-symbols-outlined text-primary animate-spin">progress_activity</span>
            <span className="font-body-sm text-on-surface-variant">Wyszukiwanie…</span>
          </div>
        )}
        {!isLoading && !query.trim() && (
          <div className="flex flex-col items-center justify-center gap-3 py-4 text-center">
            <span className="material-symbols-outlined text-3xl text-on-surface-variant">travel_explore</span>
            <p className="font-body-md text-on-surface-variant">Wpisz nazwę miejsca, aby je znaleźć</p>
          </div>
        )}
        {!isLoading && query.trim() && results.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-3 py-4 text-center">
            <span className="material-symbols-outlined text-3xl text-on-surface-variant">search_off</span>
            <p className="font-body-md text-on-surface-variant">Brak wyników</p>
          </div>
        )}
        {!isLoading && results.length > 0 && (
          <ul className="flex flex-col gap-1.5">
            {results.map((place) => {
              const isSelected = selected?.lat === place.lat && selected?.lng === place.lng && selected?.name === place.name
              return (
                <li
                  key={`${place.lat}-${place.lng}-${place.name}`}
                  onClick={() => setSelected(place)}
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
                    <span className="font-body-md text-on-surface truncate">{place.name}</span>
                    {place.description && (
                      <span className="font-body-sm text-on-surface-variant line-clamp-1">{place.description}</span>
                    )}
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </div>
      <button
        onClick={handleConfirm}
        disabled={!selected}
        className="flex-shrink-0 w-full py-2 rounded-full border border-primary text-primary font-title-md disabled:opacity-40 disabled:cursor-not-allowed hover:bg-primary/10 transition-all flex items-center justify-center gap-2"
      >
        <span className="material-symbols-outlined text-xl">arrow_forward</span>
        Dalej
      </button>
    </div>
  )
}
