import { useState, useEffect } from 'react'
import { APIProvider } from '@vis.gl/react-google-maps'
import api from '@/network/axios'
import { NavigatorSummaryCard } from '@/components/profile/NavigatorSummaryCard'
import { RouteHistoryItem } from '@/components/profile/RouteHistoryItem'
import { RouteSummaryModal } from '@/components/map/RouteSummaryModal'
import avatarPlaceholder from '@/assets/avatar_placeholder.svg'

export interface RouteHistoryItemData {
  id: string
  name: string
  date: string
  distance_km: number
  duration_minutes: number
}

interface UserStats {
  name: string
  avatar_url: string | null
  total_explorations: number
  total_distance_km: number
  total_duration_minutes: number
}

interface RouteHistoryResponse {
  user: UserStats
  routes: RouteHistoryItemData[]
}

const formatDuration = (mins: number) => {
  if (mins === 0) return '0m'
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('pl-PL', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

export function Profile() {
  const [data, setData] = useState<RouteHistoryResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [selectedRoute, setSelectedRoute] = useState<RouteHistoryItemData | null>(null)
  const API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''

  useEffect(() => {
    api
      .get<RouteHistoryResponse>('/web/history')
      .then((res) => setData(res.data))
      .catch((err) => console.error('Błąd pobierania historii:', err))
      .finally(() => setIsLoading(false))
  }, [])

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-background">
        <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    )
  }

  if (!data) return null

  const { user, routes } = data

  return (
    <APIProvider apiKey={API_KEY}>
      <main className="flex-1 overflow-y-auto bg-background p-md md:p-lg relative">
        <div className="max-w-5xl mx-auto w-full">
          <div className="flex flex-col gap-sm mb-lg">
            <h2 className="font-headline-lg text-headline-lg text-on-surface">Mój profil</h2>
            <p className="font-body-md text-body-md text-on-surface-variant">
              Przejrzyj szczegóły odbytych podróży.
            </p>
          </div>

          <NavigatorSummaryCard
            avatarSrc={user.avatar_url || avatarPlaceholder}
            name={user.name || 'Podróżnik'}
            role="Podróżnik"
            explorationCount={user.total_explorations}
            stats={[
              {
                icon: 'straighten',
                label: 'Łączny dystans',
                value: `${user.total_distance_km} km`,
              },
              {
                icon: 'timer',
                label: 'Łączny czas',
                value: formatDuration(user.total_duration_minutes),
              },
            ]}
          />

          <div className="flex flex-col gap-sm">
            {routes.length === 0 ? (
              <div className="text-center py-12 text-on-surface-variant bg-surface-container rounded-xl border border-dashed border-outline-variant">
                Nie odbyłeś żadnych podróży. Rozpocznij swoją pierwszą wycieczkę, korzystając z aplikacji mobilnej!
              </div>
            ) : (
              routes.map((route) => (
                <RouteHistoryItem
                  key={route.id}
                  icon="route"
                  title={route.name || 'Podróż'}
                  date={formatDate(route.date)}
                  distance={`${route.distance_km} km`}
                  duration={formatDuration(route.duration_minutes)}
                  onViewDetails={() => setSelectedRoute(route)}
                />
              ))
            )}
          </div>
        </div>

        <RouteSummaryModal route={selectedRoute} onClose={() => setSelectedRoute(null)} />
      </main>
    </APIProvider>
  )
}
