const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8000'

export const config = {
  apiUrl: API_BASE,
  ssoLoginUrl: `${API_BASE}/auth/keycloak/login`,
} as const
