import { useEffect, useMemo, useState } from 'react'
import api from '@/network/axios'
import { type POI, SharedMap } from '@/components/map/SharedMap'
import { SummaryStatCard } from '@/components/map/SummaryStatCard'
import { RouteTimeline, type TimelinePoint } from '@/components/map/RouteTimeline'
import type { RouteHistoryItemData } from '@/modules/profile/Profile'

interface RouteSummaryModalProps {
  route: RouteHistoryItemData | null
  onClose: () => void
}

interface RoutePoiResponse {
  id: string
  poi_id: string | null
  name: string
  lat: number
  lng: number
  description: string | null
}

interface RouteMapResponse {
  geojson: unknown
  pois: RoutePoiResponse[]
  poi_count: number
  started_at: string
}

export function RouteSummaryModal({ route, onClose }: RouteSummaryModalProps) {
  const [mapDetails, setMapDetails] = useState<RouteMapResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [delayedWaypoints, setDelayedWaypoints] = useState<
    { lat: number; lng: number }[] | undefined
  >(undefined)
  const [calculatedDistanceM, setCalculatedDistanceM] = useState<number | null>(null)

  useEffect(() => {
    setCalculatedDistanceM(null)

    if (!route) {
      setMapDetails(null)
      return
    }

    setIsLoading(true)
    api
      .get<RouteMapResponse>(`/route/${route.id}/map`)
      .then((res) => setMapDetails(res.data))
      .catch((err) => console.error('Error while fetching route map:', err))
      .finally(() => setIsLoading(false))
  }, [route])

  const timelinePoints: TimelinePoint[] = useMemo(() => {
    if (!mapDetails) return []
    return mapDetails.pois.map((p) => ({
      id: p.id,
      title: p.name || 'Nieznany punkt',
    }))
  }, [mapDetails])

  const mapPois: POI[] = useMemo(() => {
    if (!mapDetails) return []
    return mapDetails.pois.map((p, i) => {
      const isStart = i === 0
      const isEnd = i === mapDetails.pois.length - 1 && mapDetails.pois.length > 1

      let category: POI['category'] = 'history'
      if (isStart) category = 'first'
      if (isEnd) category = 'last'

      return {
        id: p.id,
        lat: p.lat,
        lng: p.lng,
        title: `${i + 1}. ${p.name}`, // Number + point name (for clarity)
        category: category,
        index: i + 1,
      }
    })
  }, [mapDetails])

  const isPlanned = useMemo(() => {
    return mapDetails ? !mapDetails.geojson : false
  }, [mapDetails])

  useEffect(() => {
    if (isPlanned && mapPois.length >= 2) {
      const timer = setTimeout(() => {
        setDelayedWaypoints(mapPois.map((p) => ({ lat: p.lat, lng: p.lng })))
      }, 300)
      return () => clearTimeout(timer)
    } else {
      setDelayedWaypoints(undefined)
    }
  }, [isPlanned, mapPois])

  const mapCenter =
    mapPois.length > 0
      ? { lat: mapPois[0].lat, lng: mapPois[0].lng }
      : { lat: 51.1092, lng: 17.0607 }

  let displayDistance = '0'
  let displayDurationText = '0:00'

  if (route) {
    if (isPlanned) {
      if (calculatedDistanceM !== null) {
        displayDistance = (calculatedDistanceM / 1000).toFixed(2)
        const totalMinutes = Math.round((calculatedDistanceM / 1000) * 30) // TODO: Make better estimation algorithm (or implement it on backend server). Currently: 1km ~ 30 min of walking
        const h = Math.floor(totalMinutes / 60)
        const m = totalMinutes % 60
        displayDurationText =
          h > 0 ? `${h}:${m.toString().padStart(2, '0')}` : `0:${m.toString().padStart(2, '0')}`
      } else {
        displayDistance = '...'
        displayDurationText = '...'
      }
    } else {
      displayDistance = route.distance_km.toString()
      const formattedHours = Math.floor(route.duration_minutes / 60)
      const formattedMinutes = route.duration_minutes % 60
      displayDurationText =
        formattedHours > 0
          ? `${formattedHours}:${formattedMinutes.toString().padStart(2, '0')}`
          : `0:${formattedMinutes.toString().padStart(2, '0')}`
    }
  }

  const formattedDate = route
    ? new Date(route.date).toLocaleDateString('pl-PL', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      })
    : ''

  if (!route) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 md:p-6 overflow-hidden">
      <div
        className="absolute inset-0 bg-black/80 cursor-pointer pointer-events-auto"
        onClick={onClose}
      />

      <div className="relative z-20 w-full max-w-5xl bg-surface-container-low rounded-xl shadow-[0_8px_32px_rgba(208,188,255,0.1)] border border-surface-variant overflow-hidden flex flex-col md:flex-row h-[90vh] md:h-auto md:max-h-160 pointer-events-auto animate-in fade-in zoom-in duration-200">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-30 w-10 h-10 flex items-center justify-center rounded-full bg-surface-container-high text-on-surface-variant hover:text-primary hover:bg-surface-container-highest transition-colors shadow-lg cursor-pointer"
        >
          <span className="material-symbols-outlined">close</span>
        </button>

        <section className="relative w-full md:w-3/5 h-64 md:h-auto min-h-75 shrink-0 bg-surface-dim overflow-hidden">
          {isLoading ? (
            <div className="absolute inset-0 flex items-center justify-center bg-surface-container/50">
              <span className="material-symbols-outlined animate-spin text-primary text-4xl">
                progress_activity
              </span>
            </div>
          ) : (
            <div className="absolute inset-0">
              <SharedMap
                mapId="DEMO_MAP_ID"
                pois={mapPois}
                center={mapCenter}
                zoom={15}
                interactive={true}
                routeWaypoints={delayedWaypoints}
                onRouteReady={setCalculatedDistanceM}
              />
            </div>
          )}
        </section>

        <section className="flex-1 flex flex-col p-6 md:p-8 bg-surface-container-low overflow-y-auto z-10 relative">
          <header className="mb-6">
            <div className="flex items-center gap-2 mb-1">
              <span
                className={`px-2 py-1 rounded-md font-label-sm uppercase tracking-wider text-label-sm ${isPlanned ? 'bg-blue-500/20 text-blue-400' : 'bg-primary-container/20 text-primary'}`}
              >
                {isPlanned ? 'Planowane' : 'Zakończone'}
              </span>
              <span className="text-on-surface-variant font-body-md text-sm">{formattedDate}</span>
            </div>
            <h1 className="text-2xl md:text-3xl text-on-surface font-bold tracking-tight mb-2">
              {route.name || 'Wycieczka'}
            </h1>

            {!isLoading && timelinePoints.length > 0 && <RouteTimeline points={timelinePoints} />}

            {!isLoading && timelinePoints.length === 0 && (
              <p className="text-on-surface-variant italic mt-4 text-sm">
                Brak szczegółowych punktów zapisanych dla tej trasy.
              </p>
            )}
          </header>

          <div className="h-px w-full bg-surface-variant my-4 opacity-50" />

          <div className="grid grid-cols-2 gap-4 mb-6">
            <SummaryStatCard
              icon="route"
              label="Dystans"
              value={displayDistance}
              unit="km"
              colorClass="text-primary"
            />
            <SummaryStatCard
              icon="schedule"
              label="Czas"
              value={displayDurationText}
              unit="h"
              colorClass="text-secondary"
            />
          </div>
        </section>
      </div>
    </div>
  )
}
