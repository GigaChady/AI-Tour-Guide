import { useState } from 'react'
import { Map, AdvancedMarker, Pin, InfoWindow, useMap } from '@vis.gl/react-google-maps'
import { useWalkingRoute } from '@/components/route-planner/useWalkingRoute'

export interface POI {
  id: string
  lat: number
  lng: number
  title: string
  category: 'history' | 'architecture' | 'curiosities' | 'planning' | 'saved' | 'selected'
}

interface SharedMapProps {
  mapId: string
  pois: POI[]
  center?: { lat: number; lng: number }
  zoom?: number
  interactive?: boolean
  selectedPoint?: { lat: number; lng: number }
  routeWaypoints?: { lat: number; lng: number }[]
  onRouteReady?: (distanceM: number | null) => void
  onPoiClick?: (poi: POI) => void
  onMapClick?: (lat: number, lng: number) => void
}

interface PinColors {
  background: string
  borderColor: string
  glyphColor: string
}

function getPinColors(category: POI['category']): PinColors {
  if (category === 'history') return { background: '#e9ddff', borderColor: '#37265e', glyphColor: '#37265e' }
  if (category === 'architecture') return { background: '#efb8c8', borderColor: '#492532', glyphColor: '#492532' }
  if (category === 'planning') return { background: '#b9f6ca', borderColor: '#1b5e20', glyphColor: '#1b5e20' }
  if (category === 'saved') return { background: '#ffe082', borderColor: '#e65100', glyphColor: '#e65100' }
  if (category === 'selected') return { background: '#d0bcff', borderColor: '#4d3d76', glyphColor: '#4d3d76' }
  return { background: '#d0bcff', borderColor: '#210f48', glyphColor: '#210f48' }
}

export function SharedMap({
  mapId,
  pois,
  center = { lat: 51.11, lng: 17.061 },
  zoom = 16,
  interactive = true,
  selectedPoint,
  routeWaypoints,
  onRouteReady,
  onPoiClick,
  onMapClick,
}: SharedMapProps) {
  const map = useMap()
  useWalkingRoute(routeWaypoints ?? [], onRouteReady)
  const [activeId, setActiveId] = useState<string | null>(null)

  const handleMyLocation = () => {
    if (navigator.geolocation && map) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          map.panTo({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          })
          map.setZoom(17)
        },
        () => {
          alert('Error: The Geolocation service failed.')
        },
      )
    } else {
      alert("Error: Your browser doesn't support geolocation.")
    }
  }

  return (
    <div className="relative w-full h-full">
      <Map
        mapId={mapId}
        defaultZoom={zoom}
        defaultCenter={center}
        disableDefaultUI={true}
        clickableIcons={false}
        zoomControl={interactive}
        mapTypeControl={false}
        streetViewControl={false}
        fullscreenControl={false}
        gestureHandling={interactive ? 'greedy' : 'none'}
        onClick={(e) => e.detail.latLng && onMapClick?.(e.detail.latLng.lat, e.detail.latLng.lng)}
      >
        {selectedPoint && (
          <AdvancedMarker position={selectedPoint}>
            <Pin background="#d0bcff" borderColor="#210f48" glyphColor="#210f48" />
          </AdvancedMarker>
        )}

        {pois.map((poi) => (
          <AdvancedMarker
            key={poi.id}
            position={{ lat: poi.lat, lng: poi.lng }}
            onClick={() => {
              setActiveId((prev) => (prev === poi.id ? null : poi.id))
              if (interactive && onPoiClick) onPoiClick(poi)
            }}
          >
            <div className="relative flex flex-col items-center">
              {activeId === poi.id && (
                <div className="absolute bottom-full mb-2 px-2 py-1 rounded-lg bg-surface-container-highest text-on-surface text-xs font-medium whitespace-nowrap shadow-md pointer-events-none">
                  {poi.title}
                </div>
              )}
              <Pin {...getPinColors(poi.category)} />
            </div>
          </AdvancedMarker>
        ))}
      </Map>

      {interactive && (
        <button
          onClick={handleMyLocation}
          title="Pokaż moją lokalizację"
          className="absolute right-2.5 bottom-27.5 w-10 h-10 bg-white text-[#666] rounded-sm shadow-[0_1px_4px_rgba(0,0,0,0.3)] flex items-center justify-center hover:text-[#333] transition-colors pointer-events-auto"
        >
          <span
            className="material-symbols-outlined text-xl"
            style={{ fontVariationSettings: "'FILL' 1" }}
          >
            my_location
          </span>
        </button>
      )}
    </div>
  )
}
