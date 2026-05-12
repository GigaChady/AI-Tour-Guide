import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { TextBox } from '@/components/ui/TextBox'
import { useUpdateUser } from '@/hooks/useAdminQueries'

interface EditUserModalProps {
  user: { name: string; email: string; id: string; fullId: string; isActive: boolean } | null
  onClose: () => void
}

export function EditUserModal({ user, onClose }: EditUserModalProps) {
  const [name, setName] = useState(user?.name ?? '')
  const [email, setEmail] = useState(user?.email ?? '')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isActive, setIsActive] = useState(user?.isActive ?? true)
  const [error, setError] = useState<string | null>(null)

  const updateUser = useUpdateUser()

  useEffect(() => {
    if (user) {
      setName(user.name)
      setEmail(user.email)
      setIsActive(user.isActive)
      setNewPassword('')
      setConfirmPassword('')
      setError(null)
    }
  }, [user?.fullId])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!user) return
    setError(null)

    if (newPassword && newPassword !== confirmPassword) {
      setError('Hasła nie są zgodne.')
      return
    }

    const payload: Record<string, unknown> = { id: user.fullId }
    if (name !== user.name) payload.name = name
    if (email !== user.email) payload.new_email = email
    if (newPassword) payload.new_password = newPassword
    if (isActive !== user.isActive) payload.is_active = isActive

    try {
      await updateUser.mutateAsync(payload as Parameters<typeof updateUser.mutateAsync>[0])
      onClose()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      setError(msg ?? 'Wystąpił błąd podczas zapisywania zmian.')
    }
  }

  return (
    <Modal isOpen={user !== null} onClose={onClose}>
      <div className="w-full bg-surface-container p-8 rounded-xl border border-outline-variant/20 relative overflow-hidden">
        <Button
          variant="icon"
          icon="arrow_back"
          className="absolute top-4 left-4 z-20"
          onClick={onClose}
        />

        <div className="absolute inset-0 bg-primary/5 pointer-events-none" />

        <div className="relative z-10 text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-primary/10 text-primary mb-4">
            <span
              className="material-symbols-outlined text-[32px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              manage_accounts
            </span>
          </div>
          <h1 className="font-headline-md text-headline-md text-on-surface">Zmień dane użytkownika</h1>
          <p className="font-body-md text-body-md text-on-surface-variant mt-2">
            Bezpiecznie zaktualizuj informacje o koncie.
          </p>
        </div>

        <form className="space-y-6 relative z-10" onSubmit={handleSubmit}>
          <TextBox
            id="fullName"
            label="Pełne imię i nazwisko"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <TextBox
            id="email"
            label="Adres e-mail"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <TextBox
            id="newPassword"
            label="Nowe hasło"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
          <TextBox
            id="confirmPassword"
            label="Potwierdź nowe hasło"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />

          <div className="flex items-center justify-between px-1 py-2">
            <div>
              <p className="font-body-lg text-body-lg text-on-surface">Konto aktywne</p>
              <p className="font-body-sm text-body-sm text-on-surface-variant">
                Nieaktywni użytkownicy nie mogą się logować.
              </p>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={isActive}
              onClick={() => setIsActive((v) => !v)}
              className={`relative w-12 h-7 rounded-full transition-colors duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary ${
                isActive ? 'bg-primary' : 'bg-outline'
              }`}
            >
              <span
                className={`absolute top-1 left-1 w-5 h-5 rounded-full bg-on-primary shadow transition-transform duration-200 ${
                  isActive ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {error && (
            <p className="font-body-sm text-body-sm text-error">{error}</p>
          )}

          <div className="pt-4">
            <Button icon="save" disabled={updateUser.isPending}>
              {updateUser.isPending ? 'Zapisywanie…' : 'Zapisz zmiany'}
            </Button>
          </div>
        </form>
      </div>
    </Modal>
  )
}
