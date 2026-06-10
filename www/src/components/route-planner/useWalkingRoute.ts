import { useEffect, useRef, useState } from 'react'
import { useMap, useMapsLibrary } from '@vis.gl/react-google-maps'

export function useWalkingRoute(
  waypoints: { lat: number; lng: number }[],
  onRouteReady?: (distanceM: number | null) => void,
) {
  const map = useMap()
  const routesLib = useMapsLibrary('routes')
  const rendererRef = useRef<google.maps.DirectionsRenderer | null>(null)
  const [totalDistanceM, setTotalDistanceM] = useState<number | null>(null)

  useEffect(() => {
    if (!routesLib || !map) return
    if (!rendererRef.current) {
      rendererRef.current = new routesLib.DirectionsRenderer({
        suppressMarkers: true,
        polylineOptions: { strokeColor: '#6750A4', strokeWeight: 4, strokeOpacity: 0.8 },
      })
      rendererRef.current.setMap(map)
    }

    if (waypoints.length < 2) {
      rendererRef.current.setDirections({ routes: [] } as any)
      setTotalDistanceM(null)
      return
    }

    const service = new routesLib.DirectionsService()
    const origin = waypoints[0]
    const destination = waypoints[waypoints.length - 1]
    const stops = waypoints.slice(1, -1).map((p) => ({
      location: new google.maps.LatLng(p.lat, p.lng),
      stopover: true,
    }))

    const handleFailure = (status: string) => {
      console.warn(`Directions request failed: ${status}`)
      setTotalDistanceM(null)
      onRouteReady?.(null)
    }

    try {
      service.route(
        {
          origin: new google.maps.LatLng(origin.lat, origin.lng),
          destination: new google.maps.LatLng(destination.lat, destination.lng),
          waypoints: stops,
          travelMode: routesLib.TravelMode.WALKING,
        },
        (result, status) => {
          if (status === 'OK' && result && rendererRef.current) {
            rendererRef.current.setDirections(result)
            const metres = result.routes[0]?.legs.reduce(
              (sum, leg) => sum + (leg.distance?.value ?? 0), 0,
            ) ?? null
            setTotalDistanceM(metres)
            onRouteReady?.(metres)
          } else {
            handleFailure(status)
          }
        },
      )
    } catch (err) {
      handleFailure(err instanceof Error ? err.message : String(err))
    }
  }, [routesLib, map, waypoints, onRouteReady])

  useEffect(() => {
    return () => rendererRef.current?.setMap(null)
  }, [])

  return { totalDistanceM }
}
