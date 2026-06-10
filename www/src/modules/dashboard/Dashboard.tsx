import { useState, useEffect } from 'react'
import { APIProvider } from '@vis.gl/react-google-maps'
import api from '@/network/axios'
import { StatsCard } from '@/components/dashboard/StatsCard'
import { ExpeditionItem } from '@/components/dashboard/ExpeditionItem'
import { RouteSummaryModal } from '@/components/map/RouteSummaryModal'
import type { RouteHistoryItemData } from '@/modules/profile/Profile'

interface DashboardStats {
  total_countries: number
  total_cities: number
  total_distance_km: number
  total_duration_minutes: number
}

interface DashboardExpedition {
  id: string
  name: string
  date: string
}

interface DashboardResponse {
  stats: DashboardStats
  recent_expeditions: DashboardExpedition[]
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('pl-PL', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

const formatDuration = (mins: number) => {
  if (mins === 0) return '0h'
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

export function Dashboard() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [selectedRoute, setSelectedRoute] = useState<RouteHistoryItemData | null>(null)

  const API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''

  useEffect(() => {
    api
      .get<DashboardResponse>('/web/dashboard')
      .then((res) => setData(res.data))
      .catch((err) => console.error('Error while fetching dashboard data:', err))
      .finally(() => setIsLoading(false))
  }, [])

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-bgDark">
        <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    )
  }

  if (!data) return null

  const { stats, recent_expeditions } = data

  const statsCards = [
    {
      icon: 'fa-solid fa-earth-americas',
      label: 'Odwiedzone kraje',
      value: stats.total_countries.toString(),
    },
    { icon: 'fa-solid fa-city', label: 'Odwiedzone miasta', value: stats.total_cities.toString() },
    {
      icon: 'fa-solid fa-route',
      label: 'Przebyte kilometry',
      value: stats.total_distance_km.toString(),
      valueClassName: 'text-emerald-400',
    },
    {
      icon: 'fa-solid fa-clock',
      label: 'Czas w podróży',
      value: formatDuration(stats.total_duration_minutes),
      valueClassName: 'text-blue-400',
    },
  ]

  return (
    <APIProvider apiKey={API_KEY}>
      <main className="flex-1 overflow-y-auto bg-bgDark p-8 lg:p-12 relative">
        <div className="max-w-6xl mx-auto space-y-10">
          <header className="border-b border-gray-800 pb-6">
            <h1 className="text-3xl font-semibold text-white tracking-tight">
              Witaj z powrotem, Podróżniku!
            </h1>
          </header>

          <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {statsCards.map((stat) => (
              <StatsCard key={stat.label} {...stat} />
            ))}
          </section>

          <section>
            <div className="flex items-center space-x-2 mb-6 text-white">
              <i className="fa-solid fa-clock-rotate-left" />
              <h2 className="text-xl font-medium">Zaplanowane podróże</h2>
            </div>

            <div className="space-y-4">
              {recent_expeditions.length === 0 ? (
                <div className="text-center py-10 text-gray-500 bg-cardDark rounded-2xl border border-dashed border-gray-800">
                  Brak zaplanowanych podróży.
                </div>
              ) : (
                recent_expeditions.map((exp) => (
                  <ExpeditionItem
                    key={exp.id}
                    city={exp.name || 'Wycieczka'}
                    date={formatDate(exp.date)}
                    avatar={{
                      type: 'icon',
                      iconClass: 'fa-solid fa-earth-americas',
                      iconColor: 'text-purple-300',
                      bgClass: 'bg-purple-900/50',
                      borderClass: 'border border-purple-500/30',
                    }}
                    onClick={() =>
                      setSelectedRoute({
                        id: exp.id,
                        name: exp.name || 'Brak trasy',
                        date: exp.date,
                        distance_km: 0,
                        duration_minutes: 0,
                      })
                    }
                  />
                ))
              )}
            </div>
          </section>
        </div>

        <RouteSummaryModal route={selectedRoute} onClose={() => setSelectedRoute(null)} />
      </main>
    </APIProvider>
  )
}
