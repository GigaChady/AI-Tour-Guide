import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import type { ReactNode } from 'react'

export function PrivateRoute({ children }: { children: ReactNode }) {
  const authenticated = useAuthStore((s) => s.authenticated)
  return authenticated ? <>{children}</> : <Navigate to="/login" replace />
}
