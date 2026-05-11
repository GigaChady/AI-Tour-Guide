import { NavigatorSummaryCard } from '@/components/profile/NavigatorSummaryCard'
import { RouteHistoryItem } from '@/components/profile/RouteHistoryItem'
import avatarPlaceholder from '@/assets/avatar_placeholder.svg'

const navigator = {
  avatarSrc: avatarPlaceholder,
  name: 'Traveler One',
  role: 'Current Navigator',
  explorationCount: 3,
  stats: [
    { icon: 'straighten', label: 'Total Distance', value: '21.4 km' },
    { icon: 'timer', label: 'Total Time', value: '7h 10m' },
  ],
}

const routes = [
  {
    icon: 'route',
    title: 'Neon Nights Shibuya',
    date: 'Oct 24, 2023',
    distance: '5.2 km',
    duration: '1h 45m',
  },
  {
    icon: 'landscape',
    title: 'Alpine Ridge Traverse',
    date: 'Oct 18, 2023',
    distance: '12.4 km',
    duration: '4h 15m',
  },
  {
    icon: 'location_city',
    title: 'Historic Downtown Loop',
    date: 'Oct 12, 2023',
    distance: '3.8 km',
    duration: '1h 10m',
  },
]

export function Profile() {
  return (
    <main className="flex-1 overflow-y-auto bg-background p-md md:p-lg">
      <div className="max-w-5xl mx-auto w-full">
        <div className="flex flex-col gap-sm mb-lg">
          <h2 className="font-headline-lg text-headline-lg text-on-surface">Route History</h2>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Review your past explorations and detailed tour metrics.
          </p>
        </div>

        <NavigatorSummaryCard {...navigator} />

        <div className="flex flex-col gap-sm">
          {routes.map((route) => (
            <RouteHistoryItem key={route.title} {...route} />
          ))}
        </div>
      </div>
    </main>
  )
}
