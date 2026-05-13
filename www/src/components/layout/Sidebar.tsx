import { Link, useLocation, useNavigate } from 'react-router-dom'
import sidebarImage from '@/assets/sidebar_image.png'
import { useAuthStore } from '@/store/authStore'

const baseNavItems = [
  { icon: 'fa-solid fa-list-ul', label: 'PULPIT', to: '/' },
  { icon: 'fa-regular fa-user', label: 'MÓJ PROFIL', to: '/profile' },
  { icon: 'fa-regular fa-map', label: 'EKSPLORACJA MAPY', to: '/map-explorer' },
  { icon: 'fa-solid fa-gear', label: 'USTAWIENIA', to: '/preferences' },
]

const adminNavItem = { icon: 'fa-solid fa-shield-halved', label: 'PANEL ADMINA', to: '/admin' }

function NavItem({ icon, label, to }: { icon: string; label: string; to: string }) {
  const { pathname } = useLocation()
  const active = pathname === to

  return (
    <Link
      to={to}
      className={
        active
          ? 'flex items-center space-x-3 px-4 py-3 bg-primaryAccent text-bgDark rounded-xl font-medium transition-colors'
          : 'flex items-center space-x-3 px-4 py-3 text-textMuted hover:text-textMain hover:bg-hoverHighlight rounded-xl font-medium transition-colors'
      }
    >
      <i className={`${icon} w-5 text-center`} />
      <span>{label}</span>
    </Link>
  )
}

export function Sidebar() {
  const logout = useAuthStore((s) => s.logout)
  const isAdmin = useAuthStore((s) => s.isAdmin)
  const navigate = useNavigate()

  const navItems = isAdmin ? [...baseNavItems, adminNavItem] : baseNavItems

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <aside className="w-64 bg-sidebarDark flex flex-col justify-between flex-shrink-0">
      <div>
        <div className="p-6 flex items-center space-x-3 mb-6">
          <img
            alt="AI Tour Guide Logo"
            className="w-10 h-10 rounded-full object-cover border-2 border-primaryAccent"
            src={sidebarImage}
          />
          <span className="text-white font-semibold text-lg tracking-wide">AI Tour Guide</span>
        </div>
        <nav className="px-4 space-y-2">
          {navItems.map((item) => (
            <NavItem key={item.label} {...item} />
          ))}
        </nav>
      </div>
      <div className="p-4">
        <button
          onClick={handleLogout}
          className="w-full flex items-center space-x-3 px-4 py-3 text-textMuted hover:text-textMain hover:bg-hoverHighlight rounded-xl font-medium transition-colors"
        >
          <i className="fa-solid fa-arrow-right-from-bracket w-5 text-center" />
          <span>WYLOGUJ</span>
        </button>
      </div>
    </aside>
  )
}
