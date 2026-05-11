import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { TextBox } from '@/components/ui/TextBox'

interface AddUserModalProps {
  isOpen: boolean
  onClose: () => void
}

export function AddUserModal({ isOpen, onClose }: AddUserModalProps) {
  return (
    <Modal isOpen={isOpen} onClose={onClose}>
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
              person_add
            </span>
          </div>
          <h1 className="font-headline-md text-headline-md text-on-surface">Add New User</h1>
          <p className="font-body-md text-body-md text-on-surface-variant mt-2">
            Create a new account and assign a role.
          </p>
        </div>

        <form className="space-y-6 relative z-10">
          <TextBox id="newFullName" label="Full Name" />
          <TextBox id="newEmail" label="Email Address" type="email" />
          <select
            id="newRole"
            defaultValue=""
            className="w-full bg-surface border border-outline rounded-lg px-4 py-3 text-on-surface font-body-md focus:outline-none focus:border-primary transition-colors appearance-none cursor-pointer"
          >
            <option value="" disabled>
              Select a role
            </option>
            <option value="admin">Administrator</option>
            <option value="editor">Content Editor</option>
            <option value="viewer">Read-Only Viewer</option>
          </select>
          <TextBox id="newPassword" label="Temporary Password" type="password" />
          <div className="pt-4">
            <Button icon="person_add">Create User</Button>
          </div>
        </form>
      </div>
    </Modal>
  )
}
