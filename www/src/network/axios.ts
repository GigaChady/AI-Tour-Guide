import axios from 'axios'
import { config } from '@/config'
import { useAuthStore } from '@/store/authStore'

const api = axios.create({
  baseURL: config.apiUrl,
})

api.interceptors.request.use((req) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    req.headers.Authorization = `Bearer ${token}`
  }
  return req
})

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const originalRequest = error.config
    // Response has 401 status code and it is not a retry request
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      const refreshToken = useAuthStore.getState().refreshToken
      // No refresh token in localStorage
      if (!refreshToken) {
        useAuthStore.getState().logout()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      // Try to refresh token
      try {
        const response = await axios.post(`${config.apiUrl}/auth/refresh`, {
          refresh_token: refreshToken,
        })

        const { access_token, refresh_token } = response.data

        // Update tokens in localStorage
        useAuthStore.setState({
          accessToken: access_token,
          refreshToken: refresh_token,
        })

        // Retry original request
        originalRequest.headers.Authorization = `Bearer ${access_token}`
        return api(originalRequest)
      } catch (refreshError) {
        // Refresh token failed, logout user
        useAuthStore.getState().logout()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  },
)

export default api
