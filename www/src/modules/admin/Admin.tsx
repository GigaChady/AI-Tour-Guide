import { ActiveDeploymentsTable } from '@/components/admin/ActiveDeploymentsTable'
import { AdminMetricCard } from '@/components/admin/AdminMetricCard'
import { NeuralProvidersCard } from '@/components/admin/NeuralProvidersCard'

const metrics = [
  { label: 'Obciążenie rdzenia', icon: 'memory', value: '42%', percentage: 42, variant: 'primary' as const },
  {
    label: 'Śr. opóźnienie',
    icon: 'speed',
    value: '124',
    unit: 'ms',
    percentage: 25,
    variant: 'tertiary' as const,
  },
]

export function Admin() {
  return (
    <div className="flex-1 flex flex-col min-h-screen overflow-y-auto bg-background">
      <div className="px-lg py-lg mt-8 md:mt-0">
        <h2 className="font-headline-lg text-headline-lg text-primary">Konfiguracja systemu</h2>
        <p className="font-body-lg text-body-lg text-on-surface-variant mt-2">
          Zarządzaj dostawcami AI i monitoruj wydajność w czasie rzeczywistym
        </p>
      </div>
      <main className="flex-1 px-lg pb-lg grid grid-cols-1 xl:grid-cols-3 gap-lg">
        <div className="xl:col-span-1 flex flex-col gap-lg">
          <div className="grid grid-cols-2 gap-md">
            {metrics.map((m) => (
              <AdminMetricCard key={m.label} {...m} />
            ))}
          </div>
          <NeuralProvidersCard />
        </div>
        <ActiveDeploymentsTable />
      </main>
    </div>
  )
}
