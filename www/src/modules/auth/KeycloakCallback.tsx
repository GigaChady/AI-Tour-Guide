import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

export function KeycloakCallback() {
  const login = useAuthStore((s) => s.login)
  const params = new URLSearchParams(window.location.search)
  const accessToken = params.get('access_token')
  const refreshToken = params.get('refresh_token')

  if (!accessToken || !refreshToken) {
    return <Navigate to="/login" replace />
  }

  login(accessToken, refreshToken)
  return <Navigate to="/" replace />
}
