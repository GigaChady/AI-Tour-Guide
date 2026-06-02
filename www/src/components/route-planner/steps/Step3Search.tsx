import { useState } from 'react'

export function Step3Search() {
  const [query, setQuery] = useState('')

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
      <div className="flex-1 flex flex-col items-center justify-center gap-3 text-center">
        <span className="material-symbols-outlined text-3xl text-on-surface-variant">travel_explore</span>
        <p className="font-body-md text-on-surface-variant">Wpisz nazwę miejsca, aby je znaleźć</p>
      </div>
    </div>
  )
}
