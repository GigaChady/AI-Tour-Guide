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
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export default api
