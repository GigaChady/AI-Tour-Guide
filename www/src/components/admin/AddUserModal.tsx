import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { TextBox } from '@/components/ui/TextBox'
import { useCreateUser } from '@/hooks/useAdminQueries'

interface AddUserModalProps {
  isOpen: boolean
  onClose: () => void
}

export function AddUserModal({ isOpen, onClose }: AddUserModalProps) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const createUser = useCreateUser()

  function reset() {
    setName('')
    setEmail('')
    setPassword('')
    setError(null)
  }

  function handleClose() {
    reset()
    onClose()
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)

    try {
      await createUser.mutateAsync({ name, email, password })
      reset()
      onClose()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      setError(msg ?? 'Wystąpił błąd podczas tworzenia użytkownika.')
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <div className="w-full bg-surface-container p-8 rounded-xl border border-outline-variant/20 relative overflow-hidden">
        <Button
          variant="icon"
          icon="arrow_back"
          className="absolute top-4 left-4 z-20"
          onClick={handleClose}
        />

        <div className="absolute inset-0 bg-primary/5 pointer-events-none" />

        <div className="relative z-10 text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-primary/10 text-primary mb-4">
            <span
              className="material-symbols-outlined text-[32px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              person_add
            </span>
          </div>
          <h1 className="font-headline-md text-headline-md text-on-surface">Dodaj nowego użytkownika</h1>
          <p className="font-body-md text-body-md text-on-surface-variant mt-2">
            Utwórz nowe konto użytkownika.
          </p>
        </div>

        <form className="space-y-6 relative z-10" onSubmit={handleSubmit}>
          <TextBox
            id="newFullName"
            label="Pełne imię i nazwisko"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <TextBox
            id="newEmail"
            label="Adres e-mail"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <TextBox
            id="newPassword"
            label="Tymczasowe hasło"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {error && (
            <p className="font-body-sm text-body-sm text-error">{error}</p>
          )}

          <div className="pt-4">
            <Button icon="person_add" disabled={createUser.isPending}>
              {createUser.isPending ? 'Tworzenie…' : 'Utwórz użytkownika'}
            </Button>
          </div>
        </form>
      </div>
    </Modal>
  )
}
