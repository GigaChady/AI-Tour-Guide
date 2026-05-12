import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import type { ReactNode } from 'react'

export function AdminRoute({ children }: { children: ReactNode }) {
  const isAdmin = useAuthStore((s) => s.isAdmin)
  return isAdmin ? <>{children}</> : <Navigate to="/" replace />
}
