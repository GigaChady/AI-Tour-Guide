import type { ButtonHTMLAttributes } from 'react'

type ButtonVariant = 'primary' | 'icon' | 'secondary' | 'ghost'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  icon?: string
}

export function Button({
  variant = 'primary',
  icon,
  children,
  className = '',
  ...props
}: ButtonProps) {
  if (variant === 'ghost') {
    return (
      <button
        {...props}
        className={`px-lg py-sm rounded-full font-label-lg text-label-lg text-primary hover:bg-primary/10 transition-colors active:scale-95 ${className}`}
      >
        {children}
      </button>
    )
  }

  if (variant === 'secondary') {
    return (
      <button
        {...props}
        className={`px-lg py-sm rounded-full font-label-lg text-label-lg bg-primary-container text-on-primary-container hover:bg-primary-container/90 transition-all active:scale-95 ${className}`}
      >
        {icon && <span className="material-symbols-outlined text-[20px]">{icon}</span>}
        {children}
      </button>
    )
  }

  if (variant === 'icon') {
    return (
      <button
        {...props}
        className={`p-2 rounded-full text-primary hover:bg-primary/10 bg-surface-container/50 backdrop-blur-sm transition-colors active:scale-95 duration-200 flex items-center justify-center ${className}`}
      >
        {icon && <span className="material-symbols-outlined">{icon}</span>}
        {children}
      </button>
    )
  }

  return (
    <button
      {...props}
      className={`w-full bg-primary text-on-primary font-label-lg text-label-lg py-3.5 rounded-full hover:bg-primary-fixed transition-all active:scale-95 flex items-center justify-center gap-2 ${className}`}
    >
      {icon && <span className="material-symbols-outlined text-[20px]">{icon}</span>}
      {children}
    </button>
  )
}
