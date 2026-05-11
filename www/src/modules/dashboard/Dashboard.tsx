import { StatsCard } from '@/components/dashboard/StatsCard'
import { ExpeditionItem } from '@/components/dashboard/ExpeditionItem'

const stats = [
  { icon: 'fa-solid fa-earth-americas', label: 'Kraje', value: '14' },
  { icon: 'fa-solid fa-plane-departure', label: 'Miasta', value: '32' },
  { icon: 'fa-solid fa-route', label: 'Kilometry', value: '42k' },
  {
    icon: 'fa-regular fa-star',
    label: 'Status elitarny',
    value: 'Gold',
    valueClassName: 'text-pink-300',
  },
]

const expeditions = [
  {
    city: 'Kyoto',
    date: 'Oct 12, 2026',
    avatar: {
      type: 'image' as const,
      src: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCXQqeM0fhzMww3MhTJvBc18nE3xjZSw1OsmH5KQzbB9N3FsHaJjWWKZohyO_KrKfiZLXsVFSW1jXksSsh-8BNWqVIEGeZK4EB4wIM-ZhaRVnxJmaUIdnx2AKS2vgVnNc5t6Nl_sO-XXIHtjLQRkAkWiAz_YGNpPqR9uALOG2vunrI200Enzx0ZOI1K9U9BG_3FnNE_G_hQphhkZoYC0N7D_krUjb__Fmo6gcDCbO0hRMj5lUvilSh_RXkAGBG9DPs2_YHRM7gBz9E',
      alt: 'Kyoto',
      bgClass: 'bg-blue-900/50',
      borderClass: 'border border-blue-500/30',
    },
  },
  {
    city: 'London',
    date: 'Sep 25, 2026',
    avatar: {
      type: 'icon' as const,
      iconClass: 'fa-solid fa-plane',
      iconColor: 'text-purple-300',
      bgClass: 'bg-purple-900/50',
      borderClass: 'border border-purple-500/30',
    },
  },
  {
    city: 'Wroclaw',
    date: 'Sep 21, 2026',
    avatar: {
      type: 'image' as const,
      src: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDwDrtVzm4LTlUr8obvvUKYKnX8u5u1HYXQ4sz0YD8cyELPPRPYf08pwtI6xAncL-pWQuWUxXRq-03lCtJ8gG3eQfZDF_cCALryAZaR0i5DYzJ6Dp119WRdFbEiFiGt3qcefJlAf5pkvdA6lqENPvmC394x8q1YkFe2raBKT0XrybIlisxBHTwCFrkno1lrYOdYp3c9uUmf5pwcSJS7YOgDSenclhbQ0nyniv62T_zRhAPro-Q42fttKh-NYU9DIqAeEdlqEcX_1vE',
      alt: 'Wroclaw',
      bgClass: 'bg-teal-900/50',
      borderClass: 'border border-teal-500/30',
    },
  },
  {
    city: 'Warsaw',
    date: 'Sep 1, 2026',
    avatar: {
      type: 'image' as const,
      src: 'https://lh3.googleusercontent.com/aida-public/AB6AXuC-nHvrwTIHzQauOz6I_Gw9_Oq5lhQC9wAEqUB9_zhXgrcw9NmXs4gyZXW6e8DM-6lwdUD9oEd7nP8r9iMeDKRvSxlfQEouzjQPVcSGsXgWFAYy0aoEDtc02ahLz1T3DbpbHGauel1b0i-Fea4Gje8ar-pB39SO7tBiqEGEWsr7caKqbgcjT75eo9j_w1mmhRyyNH8p327ZSrfFd3bcy_5SqeMDV_UQwx--FVFvBFEajKUCk5yIHa8gPsimcFhBpbEBTgf88U383Xg',
      alt: 'Warsaw',
      bgClass: 'bg-cyan-900/50',
      borderClass: 'border border-cyan-500/30',
    },
  },
  {
    city: 'London',
    date: 'March 12, 2026',
    avatar: {
      type: 'icon' as const,
      iconClass: 'fa-solid fa-plane',
      iconColor: 'text-purple-300',
      bgClass: 'bg-purple-900/50',
      borderClass: 'border border-purple-500/30',
    },
  },
  {
    city: 'Bermingam',
    date: 'March 1, 2026',
    avatar: {
      type: 'icon' as const,
      iconClass: 'fa-solid fa-plane',
      iconColor: 'text-purple-300',
      bgClass: 'bg-purple-900/50',
      borderClass: 'border border-purple-500/30',
    },
  },
  {
    city: 'Stettin',
    date: 'Sep 21, 2025',
    avatar: {
      type: 'image' as const,
      src: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAuWOoON8pKcrStC2QtgQAO1jrNFFhoocVuxM8mhmDgL6zueey2_cyVAcS0cATaPElIE2btZnOoT-rQfM_HuZEXLerPXB0KWwWrL1GHAZoPCUo6J0wees-zacpfIU8ePj70wOgxjrXhn4gLMmPzF5s1da6y6u0_oNx4xfDeSi-5iy8HLNMdFD1ea_NA13xTGc1AmKHuv6YDAB2CkM-d0d3BYz1OxUJmG738BMQqgbijpSrIXGLgvuZRtCvw8PkwJKlqugzfVqLl5uE',
      alt: 'Stettin',
      bgClass: 'bg-slate-800',
      borderClass: 'border border-slate-600/50',
    },
  },
]

export function Dashboard() {
  return (
    <main className="flex-1 overflow-y-auto bg-bgDark p-8 lg:p-12">
      <div className="max-w-6xl mx-auto space-y-10">
        <header className="border-b border-gray-800 pb-6">
          <h1 className="text-3xl font-semibold text-white tracking-tight">
            Witaj z powrotem, Podróżniku
          </h1>
        </header>

        <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {stats.map((stat) => (
            <StatsCard key={stat.label} {...stat} />
          ))}
        </section>

        <section>
          <div className="flex items-center space-x-2 mb-6 text-white">
            <i className="fa-solid fa-clock-rotate-left" />
            <h2 className="text-xl font-medium">Ostatnie ekspedycje</h2>
          </div>
          <div className="space-y-4">
            {expeditions.map((exp, i) => (
              <ExpeditionItem key={`${exp.city}-${i}`} {...exp} />
            ))}
          </div>
        </section>
      </div>
    </main>
  )
}
