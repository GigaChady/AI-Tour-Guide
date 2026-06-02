import { useReducer, useState } from 'react'
import { toast } from 'sonner'
import { APIProvider } from '@vis.gl/react-google-maps'
import api from '@/network/axios'
import { SharedMap, type POI } from '@/components/map/SharedMap'
import { PlanningWizardCard } from '@/components/route-planner/PlanningWizardCard'
import { RouteSummaryCard } from '@/components/route-planner/RouteSummaryCard'
import { useRoutePlannerSession, toSavedPoi, type ReceivedPoi, type SavedPoi } from '@/components/route-planner/useRoutePlannerSession'

const MAX_ROUTE_STOPS = 25

interface PlannerState {
  selectedPoint: { lat: number; lng: number } | null
  selectedPoiKey: string | null
  selectedPoi: ReceivedPoi | null
  savedPois: SavedPoi[]
  isRoutePending: boolean
  routeDistanceM: number | null
}

type PlannerAction =
  | { type: 'SELECT_POINT'; lat: number; lng: number }
  | { type: 'SELECT_POI'; poi: ReceivedPoi }
  | { type: 'SAVE_POI'; poi: ReceivedPoi }
  | { type: 'REMOVE_SAVED'; index: number }
  | { type: 'ROUTE_READY'; distanceM: number | null }
  | { type: 'RESET' }

const initialState: PlannerState = {
  selectedPoint: null,
  selectedPoiKey: null,
  selectedPoi: null,
  savedPois: [],
  isRoutePending: false,
  routeDistanceM: null,
}

function plannerReducer(state: PlannerState, action: PlannerAction): PlannerState {
  switch (action.type) {
    case 'SELECT_POINT':
      return { ...state, selectedPoint: { lat: action.lat, lng: action.lng }, selectedPoi: null, selectedPoiKey: null }
    case 'SELECT_POI':
      return {
        ...state,
        selectedPoint: { lat: action.poi.lat, lng: action.poi.lng },
        selectedPoiKey: action.poi.narration_id + action.poi.name,
        selectedPoi: action.poi,
      }
    case 'SAVE_POI': {
      const savedPois = [...state.savedPois, toSavedPoi(action.poi)]
      return { ...state, savedPois, selectedPoiKey: null, selectedPoi: null, isRoutePending: savedPois.length >= 2 }
    }
    case 'REMOVE_SAVED':
      return { ...state, savedPois: state.savedPois.filter((_, i) => i !== action.index) }
    case 'ROUTE_READY':
      return { ...state, isRoutePending: false, routeDistanceM: action.distanceM }
    case 'RESET':
      return initialState
  }
}

export function RoutePlanner() {
  const API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''
  const session = useRoutePlannerSession()
  const [state, dispatch] = useReducer(plannerReducer, initialState)
  const [wizardKey, setWizardKey] = useState(0)
  const { selectedPoint, selectedPoiKey, selectedPoi, savedPois, isRoutePending, routeDistanceM } = state
  const handleMapClick = session.status === 'ready'
    ? (lat: number, lng: number) => {
        dispatch({ type: 'SELECT_POINT', lat, lng })
        session.sendPlanningLocation(lat, lng)
      }
    : undefined

  const handleMapPoiClick = (poi: POI) => {
    const match = session.pois.find((p) => p.narration_id + p.name === poi.id)
    if (match) dispatch({ type: 'SELECT_POI', poi: match })
  }

  const handlePoiSelect = (poi: ReceivedPoi) => {
    dispatch({ type: 'SELECT_POI', poi })
  }

  const handleNext = (poi: ReceivedPoi) => {
    if (savedPois.length >= MAX_ROUTE_STOPS) return
    dispatch({ type: 'SAVE_POI', poi })
    session.clearPois()
    session.sendPlanningLocation(poi.lat, poi.lng)
  }

  const handleFinish = (poi: ReceivedPoi | null, planName: string) => {
    const all: SavedPoi[] = poi ? [...savedPois, toSavedPoi(poi)] : savedPois
    const payload = {
      name: planName,
      pois: all.map(({ name, lat, lng, description }) =>
        description !== null ? { name, lat, lng, description } : { lat, lng }
      ),
    }
    api.post('/web/planer', payload).then(() => {
      toast.success('Trasa została zapisana!')
      dispatch({ type: 'RESET' })
      session.closeSession()
      setWizardKey((k) => k + 1)
    }).catch(() => {
      toast.error('Nie udało się zapisać trasy.')
    })
  }

  const planningPois = [
    ...session.pois.map((p) => ({
      id: p.narration_id + p.name,
      lat: p.lat,
      lng: p.lng,
      title: p.name,
      category: (selectedPoiKey === p.narration_id + p.name ? 'selected' : 'planning') as 'selected' | 'planning',
    })),
    ...savedPois.map((p, i) => ({
      id: `saved-${p.name}-${p.lat}-${p.lng}`,
      lat: p.lat,
      lng: p.lng,
      title: p.name,
      category: 'saved' as const,
      index: i + 1,
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
            onRouteReady={(distanceM) => dispatch({ type: 'ROUTE_READY', distanceM })}
            onPoiClick={handleMapPoiClick}
            onMapClick={handleMapClick}
          />
        </div>
        <div className="absolute top-3 left-3 bottom-3 w-80 z-10 flex flex-col justify-end gap-2 pointer-events-none">
          {session.status === 'ready' && (
            <RouteSummaryCard
              pois={savedPois}
              distanceM={routeDistanceM}
              onRemove={(i) => dispatch({ type: 'REMOVE_SAVED', index: i })}
            />
          )}
          <PlanningWizardCard key={wizardKey}
            onStart={session.startSession}
            onPoiSelect={handlePoiSelect}
            onNext={handleNext}
            onFinish={handleFinish}
            pois={session.pois}
            isPoisLoading={session.isPoisLoading}
            isRoutePending={isRoutePending}
            savedCount={savedPois.length}
            maxStops={MAX_ROUTE_STOPS}
            selectedMapPoint={selectedPoint}
            selectedPoi={selectedPoi}
          />
        </div>
      </main>
    </APIProvider>
  )
}
