import { useState } from 'react'
import { APIProvider } from '@vis.gl/react-google-maps'
import { RouteSummaryModal } from './RouteSummaryModal'
import { SharedMap, type POI } from '@/components/map/SharedMap'
import { NarrationPlayer } from '@/components/map/NarrationPlayer'

// Mock POI data
const MOCK_POIS: POI[] = [
  { id: '1', lat: 51.111, lng: 17.06, title: 'Pasaż Grunwaldzki', category: 'architecture' },
  {
    id: '2',
    lat: 51.1095,
    lng: 17.0625,
    title: 'Politechnika Wrocławska',
    category: 'architecture',
  },
  { id: '3', lat: 51.1075, lng: 17.0615, title: 'Most Zwierzyniecki', category: 'history' },
]

export function MapExplorer() {
  const [isActiveSession, setIsActiveSession] = useState(false)
  const [isPlaying, setIsPlaying] = useState(true)
  const [selectedPoi, setSelectedPoi] = useState<POI | null>(MOCK_POIS[0])
  const [isSummaryOpen, setIsSummaryOpen] = useState(false)

  const API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''

  const handleEndJourney = () => {
    setIsActiveSession(false)
    setIsSummaryOpen(true)
  }

  return (
    <APIProvider apiKey={API_KEY}>
      <main className="flex-1 relative flex flex-col h-screen overflow-hidden bg-surface-container-lowest">
        {/* Tło mapy */}
        <div className="absolute inset-0">
          <SharedMap
            mapId="DEMO_MAP_ID"
            pois={isActiveSession ? MOCK_POIS : []}
            interactive={true}
            onPoiClick={setSelectedPoi}
          />
          {!isActiveSession && (
            <div className="absolute inset-0 bg-background/50 backdrop-blur-[2px] z-0 pointer-events-none" />
          )}
        </div>

        {/* Start trip overlay */}
        {!isActiveSession ? (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-10">
            <button
              onClick={() => setIsActiveSession(true)}
              className="pointer-events-auto px-8 py-4 rounded-full bg-primary text-on-primary font-headline-md shadow-[0_0_30px_rgba(208,188,255,0.4)] hover:scale-105 hover:bg-primary-fixed transition-all duration-300 flex items-center gap-3 cursor-pointer"
            >
              <span className="material-symbols-outlined text-3xl">play_circle</span>
              Rozpocznij podróż
            </button>
          </div>
        ) : (
          <>
            {/* End trip button */}
            <div className="absolute bottom-6 left-6 z-10 pointer-events-none">
              <button
                onClick={handleEndJourney}
                className="pointer-events-auto px-6 py-3 rounded-full bg-error text-on-error hover:bg-error-container hover:text-on-error-container font-semibold tracking-wide shadow-lg transition-colors flex items-center gap-2 cursor-pointer"
              >
                <span className="material-symbols-outlined">flag</span>
                Zakończ podróż
              </button>
            </div>

            {/* Narration player */}
            <div className="absolute bottom-6 left-1/2 -translate-x-1/2 w-full max-w-5xl px-4 z-10 pointer-events-none">
              <NarrationPlayer
                poiTitle={selectedPoi?.title || 'Wybierz punkt'}
                isPlaying={isPlaying}
                onTogglePlay={() => setIsPlaying(!isPlaying)}
              />
            </div>
          </>
        )}

        {/* Route summary modal */}
        <RouteSummaryModal isOpen={isSummaryOpen} onClose={() => setIsSummaryOpen(false)} />
      </main>
    </APIProvider>
  )
}
