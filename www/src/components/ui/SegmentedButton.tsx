import { Fragment } from 'react'

export interface SegmentOption<T> {
  label: string
  value: T
}

interface SegmentedButtonProps<T> {
  description: string
  options: SegmentOption<T>[]
  value: T
  onChange: (val: T) => void
}

export function SegmentedButton<T extends string | boolean>({
  description,
  options,
  value,
  onChange,
}: SegmentedButtonProps<T>) {
  return (
    <div className="flex flex-col gap-4">
      <p className="font-body-md text-body-md text-on-surface-variant">{description}</p>
      <div className="flex w-full rounded-full border border-outline-variant/30 overflow-hidden">
        {options.map((opt, index) => {
          const isActive = value === opt.value
          return (
            <Fragment key={String(opt.value)}>
              <button
                onClick={() => onChange(opt.value)}
                className={`flex-1 py-3 font-label-lg flex items-center justify-center gap-2 transition-colors ${
                  isActive
                    ? 'bg-secondary-container text-on-secondary-container'
                    : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'
                }`}
              >
                {isActive && <span className="material-symbols-outlined text-sm">check</span>}
                {opt.label}
              </button>
              {index < options.length - 1 && <div className="w-px bg-outline-variant/30" />}
            </Fragment>
          )
        })}
      </div>
    </div>
  )
}
