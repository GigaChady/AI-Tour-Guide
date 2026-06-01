import { useEffect, useState } from 'react'
import { APIProvider } from '@vis.gl/react-google-maps'
import { SharedMap } from '@/components/map/SharedMap'
import { PlanningWizardCard } from '@/components/route-planner/PlanningWizardCard'
import { RouteSummaryCard } from '@/components/route-planner/RouteSummaryCard'
import { useRoutePlannerSession, type ReceivedPoi } from '@/components/route-planner/useRoutePlannerSession'

const MAX_ROUTE_STOPS = 25

export function RoutePlanner() {
  const API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''
  const session = useRoutePlannerSession()
  const [selectedPoint, setSelectedPoint] = useState<{ lat: number; lng: number } | null>(null)
  const [selectedPoiKey, setSelectedPoiKey] = useState<string | null>(null)
  const [savedPois, setSavedPois] = useState<ReceivedPoi[]>([])
  const [isRoutePending, setIsRoutePending] = useState(false)
  const [routeDistanceM, setRouteDistanceM] = useState<number | null>(null)

  useEffect(() => {
    if (savedPois.length >= 2) setIsRoutePending(true)
  }, [savedPois])

  const handleMapClick = session.status === 'ready'
    ? (lat: number, lng: number) => {
        setSelectedPoint({ lat, lng })
        session.sendPlanningLocation(lat, lng)
      }
    : undefined

  const handlePoiSelect = (poi: ReceivedPoi) => {
    setSelectedPoint({ lat: poi.lat, lng: poi.lng })
    setSelectedPoiKey(poi.narration_id + poi.name)
  }

  const handleNext = (poi: ReceivedPoi) => {
    if (savedPois.length >= MAX_ROUTE_STOPS) return
    setSavedPois((prev) => [...prev, poi])
    setSelectedPoiKey(null)
    session.clearPois()
    session.sendPlanningLocation(poi.lat, poi.lng)
  }

  const handleFinish = (poi: ReceivedPoi | null) => {
    const all = poi ? [...savedPois, poi] : savedPois
    console.log('saved route pois', all)
  }

  const planningPois = [
    ...session.pois.map((p) => ({
      id: p.narration_id + p.name,
      lat: p.lat,
      lng: p.lng,
      title: p.name,
      category: (selectedPoiKey === p.narration_id + p.name ? 'selected' : 'planning') as 'selected' | 'planning',
    })),
    ...savedPois.map((p) => ({
      id: 'saved-' + p.narration_id + p.name,
      lat: p.lat,
      lng: p.lng,
      title: p.name,
      category: 'saved' as const,
    })),
  ]

  return (
    <APIProvider apiKey={API_KEY}>
      <main className="flex-1 relative h-screen overflow-hidden">
        <div className="absolute inset-0">
          <SharedMap
            mapId="ROUTE_PLANNER_MAP_ID"
            pois={planningPois}
            interactive={true}
            selectedPoint={selectedPoint ?? undefined}
            routeWaypoints={savedPois.map((p) => ({ lat: p.lat, lng: p.lng }))}
            onRouteReady={(distanceM) => { setIsRoutePending(false); setRouteDistanceM(distanceM) }}
            onMapClick={handleMapClick}
          />
        </div>
        {session.status === 'ready' && (
          <div className="absolute top-6 left-6 z-10 pointer-events-none">
            <RouteSummaryCard
              pois={savedPois}
              distanceM={routeDistanceM}
              onRemove={(i) => setSavedPois((prev) => prev.filter((_, idx) => idx !== i))}
            />
          </div>
        )}

        <PlanningWizardCard
          onStart={session.startSession}
          onPoiSelect={handlePoiSelect}
          onNext={handleNext}
          onFinish={handleFinish}
          pois={session.pois}
          isPoisLoading={session.isPoisLoading}
          isRoutePending={isRoutePending}
          savedCount={savedPois.length}
          maxStops={MAX_ROUTE_STOPS}
        />
      </main>
    </APIProvider>
  )
}
