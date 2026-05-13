import { ActiveDeploymentsTable } from '@/components/admin/ActiveDeploymentsTable'
import { AdminMetricCard } from '@/components/admin/AdminMetricCard'
import { NeuralProvidersCard } from '@/components/admin/NeuralProvidersCard'
import { useAdminStats } from '@/hooks/useAdminQueries'

export function Admin() {
  const { data: stats } = useAdminStats()

  const tokenK = stats ? Math.round(stats.token_spent / 1000) : 0
  const tokenPct = Math.min(Math.round((stats?.token_spent ?? 0) / 500), 100)
  const userPct = Math.min((stats?.active_users ?? 0) * 10, 100)

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
            <AdminMetricCard
              label="Aktywni użytkownicy"
              icon="group"
              value={String(stats?.active_users ?? '—')}
              percentage={userPct}
              variant="primary"
            />
            <AdminMetricCard
              label="Tokeny zużyte"
              icon="token"
              value={String(tokenK)}
              unit="k"
              percentage={tokenPct}
              variant="tertiary"
            />
          </div>
          <NeuralProvidersCard />
        </div>
        <ActiveDeploymentsTable />
      </main>
    </div>
  )
}
