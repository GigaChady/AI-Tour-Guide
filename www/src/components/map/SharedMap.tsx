import { Map, AdvancedMarker, Pin, useMap } from '@vis.gl/react-google-maps'
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
            onClick={() => onPoiClick?.(poi)}
          >
            <div className="relative flex flex-col items-center group">
              <div className="absolute bottom-full mb-3 left-1/2 -translate-x-1/2 pointer-events-none z-10 flex flex-col items-center opacity-0 group-hover:opacity-100 transition-opacity duration-150">
                <div className="bg-surface-container-highest/95 backdrop-blur-sm rounded-xl border border-outline-variant/30 shadow-[0_4px_20px_rgba(0,0,0,0.4)] px-3 py-2 min-w-max max-w-52">
                  <p className="text-on-surface text-sm font-medium leading-snug">{poi.title}</p>
                </div>
                <div className="w-2.5 h-2.5 bg-surface-container-highest rotate-45 -mt-[5px] border-r border-b border-outline-variant/30 shadow-[2px_2px_4px_rgba(0,0,0,0.15)]" />
              </div>
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
