import { useState, useEffect, useMemo } from 'react'
import { APIProvider, useMap } from '@vis.gl/react-google-maps'
import { SharedMap, type POI } from '@/components/map/SharedMap'
import { NarrationPlayer } from '@/components/map/NarrationPlayer'
import { useNarrationStream } from '@/hooks/useNarrationStream'

function MapExplorerContent() {
  const { status, transcript, words, poi, audioUrl, requestNarration } = useNarrationStream({
    autoStartTour: false,
  })
  const map = useMap()
  const activePoi = useMemo(() => {
    if (!poi) return null
    return {
      id: poi.name + poi.lat,
      title: poi.name,
      lat: poi.lat,
      lng: poi.lng,
      category: 'history' as const,
    }
  }, [poi])

  // Center map (pan to new marker)
  useEffect(() => {
    if (activePoi && map) {
      map.panTo({ lat: activePoi.lat, lng: activePoi.lng })
    }
  }, [activePoi, map])

  const [selectedPoint, setSelectedPoint] = useState<{ lat: number; lng: number } | null>(null)
  const isBusy = status === 'connecting' || status === 'generating'

  // Handle map click (clicking empty map area)
  const handleMapClick = (lat: number, lng: number) => {
    if (isBusy) return
    setSelectedPoint({ lat, lng })
    requestNarration(lat, lng)
  }

  // Handle marker click (clicking POI marker)
  const handlePoiClick = (clickedPoi: POI) => {
    if (isBusy) return
    if (map) map.panTo({ lat: clickedPoi.lat, lng: clickedPoi.lng })
  }

  return (
    <main className="flex-1 relative flex flex-col h-screen overflow-hidden bg-surface-container-lowest">
      <div className="absolute inset-0">
        <SharedMap
          mapId="DEMO_MAP_ID"
          pois={activePoi ? [activePoi] : []}
          interactive={true}
          selectedPoint={selectedPoint ?? undefined}
          onPoiClick={handlePoiClick}
          onMapClick={handleMapClick}
        />
      </div>

      {status === 'connecting' && (
        <div className="absolute top-6 left-1/2 -translate-x-1/2 z-10 bg-surface-container-high/90 backdrop-blur-md px-6 py-2 rounded-full border border-primary/20 shadow-lg flex items-center gap-3">
          <div className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
          <span className="text-on-surface text-sm font-medium tracking-wide">
            Łączenie z serwerem...
          </span>
        </div>
      )}

      <div className="absolute bottom-6 left-1/2 -translate-x-1/2 w-full max-w-5xl px-4 z-10 pointer-events-none transition-transform duration-500 ease-out">
        <NarrationPlayer
          poiTitle={
            activePoi?.title || (selectedPoint ? 'Wyszukiwanie...' : 'Nie wybrano miejsca')
          }
          transcript={transcript}
          words={words}
          audioUrl={audioUrl}
          status={status}
        />
      </div>
    </main>
  )
}

export function MapExplorer() {
  const API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''

  return (
    <APIProvider apiKey={API_KEY}>
      <MapExplorerContent />
    </APIProvider>
  )
}
