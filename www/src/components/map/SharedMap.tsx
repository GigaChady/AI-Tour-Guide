import { Map, AdvancedMarker, Pin, useMap } from '@vis.gl/react-google-maps'

export interface POI {
  id: string
  lat: number
  lng: number
  title: string
  category: 'history' | 'architecture' | 'curiosities'
}

interface SharedMapProps {
  mapId: string
  pois: POI[]
  center?: { lat: number; lng: number }
  zoom?: number
  interactive?: boolean
  onPoiClick?: (poi: POI) => void
}

export function SharedMap({
  mapId,
  pois,
  center = { lat: 51.11, lng: 17.061 },
  zoom = 16,
  interactive = true,
  onPoiClick,
}: SharedMapProps) {
  const map = useMap()

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
        zoomControl={interactive}
        mapTypeControl={false}
        streetViewControl={false}
        fullscreenControl={false}
        gestureHandling={interactive ? 'greedy' : 'none'}
      >
        {pois.map((poi) => (
          <AdvancedMarker
            key={poi.id}
            position={{ lat: poi.lat, lng: poi.lng }}
            onClick={() => {
              if (interactive && onPoiClick) onPoiClick(poi)
            }}
          >
            <Pin
              background={
                poi.category === 'history'
                  ? '#e9ddff'
                  : poi.category === 'architecture'
                    ? '#efb8c8'
                    : '#d0bcff'
              }
              borderColor={
                poi.category === 'history'
                  ? '#37265e'
                  : poi.category === 'architecture'
                    ? '#492532'
                    : '#210f48'
              }
              glyphColor={
                poi.category === 'history'
                  ? '#37265e'
                  : poi.category === 'architecture'
                    ? '#492532'
                    : '#210f48'
              }
            />
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
