import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { TextBox } from '@/components/ui/TextBox'

interface EditUserModalProps {
  user: { name: string; email: string; id: string; isActive: boolean } | null
  onClose: () => void
}

export function EditUserModal({ user, onClose }: EditUserModalProps) {
  const [isActive, setIsActive] = useState(user?.isActive ?? true)

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

        <form className="space-y-6 relative z-10">
          <TextBox id="fullName" label="Pełne imię i nazwisko" defaultValue={user?.name} />
          <TextBox id="email" label="Adres e-mail" type="email" defaultValue={user?.email} />
          <TextBox id="newPassword" label="Nowe hasło" type="password" />
          <TextBox id="confirmPassword" label="Potwierdź nowe hasło" type="password" />

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

          <div className="pt-4">
            <Button icon="save">Zapisz zmiany</Button>
          </div>
        </form>
      </div>
    </Modal>
  )
}
