import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { TextBox } from '@/components/ui/TextBox'

interface EditUserModalProps {
  user: { name: string; id: string } | null
  onClose: () => void
}

export function EditUserModal({ user, onClose }: EditUserModalProps) {
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
          <h1 className="font-headline-md text-headline-md text-on-surface">Change User Details</h1>
          <p className="font-body-md text-body-md text-on-surface-variant mt-2">
            Update your account information securely.
          </p>
        </div>

        <form className="space-y-6 relative z-10">
          <TextBox id="fullName" label="Full Name" defaultValue={user?.name} />
          <TextBox id="email" label="Email Address" type="email" />
          <TextBox id="newPassword" label="New Password" type="password" />
          <TextBox id="confirmPassword" label="Confirm New Password" type="password" />
          <div className="pt-4">
            <Button icon="save">Save Changes</Button>
          </div>
        </form>
      </div>
    </Modal>
  )
}
