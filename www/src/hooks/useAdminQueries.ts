import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api from '@/lib/axios'
import type {
  AdminDeploymentsResponse,
  AdminStatsResponse,
  AvailableProvidersResponse,
  SystemConfigResponse,
} from '@/types/admin'

export function useAdminDeployments() {
  return useQuery<AdminDeploymentsResponse>({
    queryKey: ['admin', 'deployments'],
    queryFn: () => api.get('/web/admin/deployments').then((r) => r.data),
  })
}

export function useAdminStats() {
  return useQuery<AdminStatsResponse>({
    queryKey: ['admin', 'stats'],
    queryFn: () => api.get('/web/admin/stats').then((r) => r.data),
  })
}

export function useAdminProviders() {
  return useQuery<AvailableProvidersResponse>({
    queryKey: ['admin', 'providers'],
    queryFn: () => api.get('/web/admin/providers').then((r) => r.data),
  })
}

export function useAdminConfig() {
  return useQuery<SystemConfigResponse>({
    queryKey: ['admin', 'config'],
    queryFn: () => api.get('/web/admin/config').then((r) => r.data),
  })
}

export function useUpdateAdminConfig() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: Partial<SystemConfigResponse>) =>
      api.put<SystemConfigResponse>('/web/admin/config', body).then((r) => r.data),
    onSuccess: (data) => {
      qc.setQueryData(['admin', 'config'], data)
    },
  })
}
