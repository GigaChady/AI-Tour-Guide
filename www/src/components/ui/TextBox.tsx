import type { ChangeEvent } from 'react'

interface TextBoxProps {
  id: string
  label: string
  type?: string
  defaultValue?: string
  value?: string
  onChange?: (e: ChangeEvent<HTMLInputElement>) => void
}

export function TextBox({ id, label, type = 'text', defaultValue, value, onChange }: TextBoxProps) {
  return (
    <div className="relative">
      <input
        id={id}
        type={type}
        placeholder={label}
        defaultValue={defaultValue}
        value={value}
        onChange={onChange}
        className="peer w-full bg-surface border border-outline rounded-lg px-4 py-3 text-on-surface font-body-md focus:outline-none focus:border-primary transition-all placeholder-transparent"
      />
      <label
        htmlFor={id}
        className="absolute left-3 -top-2.5 bg-surface-container peer-placeholder-shown:bg-transparent px-1 text-on-surface-variant font-label-sm text-label-sm transition-all peer-placeholder-shown:text-body-md peer-placeholder-shown:top-3.5 peer-focus:-top-2.5 peer-focus:text-label-sm peer-focus:text-primary peer-focus:bg-surface-container rounded-sm"
      >
        {label}
      </label>
    </div>
  )
}
