const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8000'

export const config = {
  apiUrl: API_BASE,
  wsUrl: API_BASE.replace(/^http/, 'ws'),
  ssoLoginUrl: `${API_BASE}/auth/keycloak/login`,
} as const
