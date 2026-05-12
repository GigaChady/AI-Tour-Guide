import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { config } from '@/config'

function decodeJwt(token: string): Record<string, unknown> {
  const payload = token.split('.')[1]
  return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
}

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  authenticated: boolean
  isAdmin: boolean
  login: (accessToken: string, refreshToken: string) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      authenticated: false,
      isAdmin: false,

      login(accessToken, refreshToken) {
        const payload = decodeJwt(accessToken)
        set({
          accessToken,
          refreshToken,
          authenticated: true,
          isAdmin: payload.is_admin === true,
        })
      },

      logout() {
        const { refreshToken } = get()
        set({ accessToken: null, refreshToken: null, authenticated: false, isAdmin: false })

        if (refreshToken) {
          fetch(`${config.apiUrl}/auth/logout`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refresh_token: refreshToken }),
          }).catch(() => {})
        }
      },
    }),
    {
      name: 'auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        authenticated: state.authenticated,
        isAdmin: state.isAdmin,
      }),
    },
  ),
)

export function authHeaders(): Record<string, string> {
  const token = useAuthStore.getState().accessToken
  return token ? { Authorization: `Bearer ${token}` } : {}
}
