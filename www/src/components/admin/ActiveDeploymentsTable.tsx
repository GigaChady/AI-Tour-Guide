import { useEffect, useRef, useState } from 'react'
import { AddUserModal } from '@/components/admin/AddUserModal'
import { EditUserModal } from '@/components/admin/EditUserModal'
import { useAdminDeployments } from '@/hooks/useAdminQueries'
import type { AdminDeploymentUser } from '@/types/admin'

type UserStatus = 'active' | 'idle'
type AvatarVariant = 'primary' | 'tertiary'

interface UserDeployment {
  initials: string
  name: string
  email: string
  id: string
  isActive: boolean
  status: UserStatus
  currentRoute: string
  avatarVariant: AvatarVariant
}

function getInitials(name: string | null, email: string): string {
  const source = name ?? email
  return source
    .split(/[\s@]/)
    .filter(Boolean)
    .slice(0, 2)
    .map((s) => s[0].toUpperCase())
    .join('')
}

function mapUser(user: AdminDeploymentUser, index: number): UserDeployment {
  return {
    initials: getInitials(user.name, user.email),
    name: user.name ?? user.email,
    email: user.email,
    id: user.id.slice(0, 8),
    isActive: user.is_active,
    status: user.on_route ? 'active' : 'idle',
    currentRoute: user.on_route ? 'Na trasie' : '—',
    avatarVariant: index % 2 === 0 ? 'primary' : 'tertiary',
  }
}

function StatusBadge({ status }: { status: UserStatus }) {
  if (status === 'active') {
    return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-container/20 text-primary-fixed-dim font-label-sm text-label-sm border border-primary/20">
        <span className="w-1.5 h-1.5 rounded-full bg-primary" />
        Aktywny
      </span>
    )
  }
  return (
    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface-container-highest text-on-surface-variant font-label-sm text-label-sm border border-white/10">
      <span className="w-1.5 h-1.5 rounded-full bg-outline" />
      Nieaktywny
    </span>
  )
}

function UserDeploymentRow({
  initials,
  name,
  email,
  id,
  isActive,
  status,
  currentRoute,
  avatarVariant,
  onEdit,
}: UserDeployment & { onEdit: (user: UserDeployment) => void }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!menuOpen) return
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuOpen])

  const avatarClasses =
    avatarVariant === 'primary'
      ? 'bg-primary/20 text-primary'
      : 'bg-tertiary/20 text-tertiary'

  return (
    <tr className="hover:bg-white/5 transition-colors group">
      <td className="p-4 pl-lg">
        <div className="flex items-center gap-3">
          <div
            className={`w-10 h-10 rounded-full flex items-center justify-center font-bold ${avatarClasses}`}
          >
            {initials}
          </div>
          <div>
            <div className="font-title-md text-title-md text-on-surface">{name}</div>
            <div className="font-body-md text-body-md text-on-surface-variant">ID: {id}</div>
          </div>
        </div>
      </td>
      <td className="p-4">
        <StatusBadge status={status} />
      </td>
      <td className="p-4 font-body-md text-body-md text-on-surface">{currentRoute}</td>
      <td className="p-4 pr-lg text-right">
        <div ref={menuRef} className="relative inline-block">
          <button
            className="p-2 text-on-surface-variant hover:text-primary transition-colors"
            onClick={() => setMenuOpen((o) => !o)}
          >
            <span className="material-symbols-outlined">more_vert</span>
          </button>
          {menuOpen && (
            <div className="absolute right-0 top-full mt-1 w-36 bg-surface-container-high border border-white/10 rounded-xl shadow-xl z-50 overflow-hidden">
              <button
                className="w-full flex items-center gap-2 px-4 py-3 text-on-surface hover:bg-white/5 transition-colors font-body-md text-body-md"
                onClick={() => {
                  setMenuOpen(false)
                  onEdit({ initials, name, email, id, isActive, status, currentRoute, avatarVariant })
                }}
              >
                <span className="material-symbols-outlined text-[18px]">edit</span>
                Edytuj
              </button>
              <button className="w-full flex items-center gap-2 px-4 py-3 text-error hover:bg-white/5 transition-colors font-body-md text-body-md">
                <span className="material-symbols-outlined text-[18px]">delete</span>
                Usuń
              </button>
            </div>
          )}
        </div>
      </td>
    </tr>
  )
}

export function ActiveDeploymentsTable() {
  const { data, isLoading, isError } = useAdminDeployments()
  const [editingUser, setEditingUser] = useState<UserDeployment | null>(null)
  const [addingUser, setAddingUser] = useState(false)

  const users = (data?.users ?? []).map(mapUser)

  return (
    <>
      <div className="xl:col-span-2 bg-surface-container rounded-[2rem] border border-white/5 shadow-lg overflow-hidden flex flex-col">
        <div className="p-lg border-b border-white/5 flex items-center justify-between bg-surface-container-low">
          <h3 className="font-title-lg text-title-lg text-on-surface">Aktywne wdrożenia</h3>
          <button
            className="bg-primary hover:bg-primary-fixed-dim text-on-primary font-label-lg text-label-lg py-2 px-6 rounded-full transition-all duration-300 flex items-center gap-2 active:scale-95"
            onClick={() => setAddingUser(true)}
          >
            <span className="material-symbols-outlined text-lg">add</span>
            Dodaj użytkownika
          </button>
        </div>
        <div className="overflow-x-auto flex-1">
          {isLoading && (
            <div className="flex items-center justify-center py-16 text-on-surface-variant">
              <span className="material-symbols-outlined animate-spin text-3xl">progress_activity</span>
            </div>
          )}
          {isError && (
            <div className="flex items-center justify-center py-16 text-error font-body-md text-body-md">
              Nie udało się wczytać danych.
            </div>
          )}
          {!isLoading && !isError && (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-surface-container/50 border-b border-white/5 font-label-lg text-label-lg text-on-surface-variant">
                  <th className="p-4 font-medium pl-lg">Użytkownik / Instancja</th>
                  <th className="p-4 font-medium">Status</th>
                  <th className="p-4 font-medium">Aktualna trasa</th>
                  <th className="p-4 font-medium text-right pr-lg">Akcje</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {users.map((user) => (
                  <UserDeploymentRow key={user.id} {...user} onEdit={setEditingUser} />
                ))}
                {users.length === 0 && (
                  <tr>
                    <td colSpan={4} className="p-8 text-center text-on-surface-variant font-body-md text-body-md">
                      Brak użytkowników.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <EditUserModal key={editingUser?.id} user={editingUser} onClose={() => setEditingUser(null)} />
      <AddUserModal isOpen={addingUser} onClose={() => setAddingUser(false)} />
    </>
  )
}
