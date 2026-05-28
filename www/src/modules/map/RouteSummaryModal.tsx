import { SharedMap } from '@/components/map/SharedMap'
import { SummaryStatCard } from '@/components/map/SummaryStatCard'
import { RouteTimeline, type TimelinePoint } from '@/components/map/RouteTimeline'

interface RouteSummaryModalProps {
  isOpen: boolean
  onClose: () => void
}

// Mock POI data
const MOCK_JOURNEY_POINTS: TimelinePoint[] = [
  { id: '1', title: 'Pasaż Grunwaldzki' },
  { id: '2', title: 'Politechnika Wrocławska' },
  { id: '3', title: 'Most Zwierzyniecki' },
]

export function RouteSummaryModal({ isOpen, onClose }: RouteSummaryModalProps) {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 md:p-6 overflow-hidden">
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-md cursor-pointer pointer-events-auto"
        onClick={onClose}
      />

      <div className="relative z-20 w-full max-w-5xl bg-surface-container-low rounded-xl shadow-[0_8px_32px_rgba(208,188,255,0.1)] border border-surface-variant overflow-hidden flex flex-col md:flex-row h-[90vh] md:h-auto md:max-h-160 pointer-events-auto animate-in fade-in zoom-in duration-200">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-30 w-10 h-10 flex items-center justify-center rounded-full bg-surface-container-high text-on-surface-variant hover:text-primary hover:bg-surface-container-highest transition-colors shadow-lg cursor-pointer"
        >
          <span className="material-symbols-outlined">close</span>
        </button>

        <section className="relative w-full md:w-3/5 h-64 md:h-160 shrink-0 bg-surface-dim overflow-hidden">
          <SharedMap
            mapId="SUMMARY_MAP_ID"
            // Mock POI data
            pois={[
              { id: 'start', lat: 51.111, lng: 17.06, title: 'Start', category: 'history' },
              { id: 'end', lat: 51.1075, lng: 17.0615, title: 'Koniec', category: 'architecture' },
            ]}
            center={{ lat: 51.1092, lng: 17.0607 }}
            zoom={15}
            interactive={false}
          />
        </section>

        <section className="flex-1 flex flex-col p-6 md:p-8 bg-surface-container-low overflow-y-auto z-10 relative">
          <header className="mb-6">
            <div className="flex items-center gap-2 mb-1">
              <span className="bg-primary-container/20 text-primary px-2 py-1 rounded-md font-label-sm uppercase tracking-wider text-label-sm">
                Zakończono
              </span>
              <span className="text-on-surface-variant font-body-md text-sm">28 Maja, 2026</span>
            </div>
            <h1 className="text-2xl md:text-3xl text-on-surface font-bold tracking-tight mb-2">
              Neon Nights Circuit
            </h1>

            {/* Timeline (visited POIs) */}
            <RouteTimeline points={MOCK_JOURNEY_POINTS} />
          </header>

          <div className="h-px w-full bg-surface-variant my-4 opacity-50" />

          {/* Summary stats */}
          <div className="grid grid-cols-2 gap-4 mb-6">
            <SummaryStatCard
              icon="route"
              label="Dystans"
              value="4.2"
              unit="km"
              colorClass="text-primary"
            />
            <SummaryStatCard
              icon="schedule"
              label="Czas"
              value="2:15"
              unit="godz"
              colorClass="text-secondary"
            />
          </div>
        </section>
      </div>
    </div>
  )
}
