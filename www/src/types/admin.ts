export interface AdminDeploymentUser {
  id: string
  name: string | null
  email: string
  is_active: boolean
  on_route: boolean
}

export interface AdminDeploymentsResponse {
  users: AdminDeploymentUser[]
}

export interface AdminStatsResponse {
  active_users: number
  token_spent: number
}

export interface ProviderOption {
  key: string
  label: string
}

export interface AvailableProvidersResponse {
  tts_providers: ProviderOption[]
  llm_providers: ProviderOption[]
}

export interface SystemConfigResponse {
  tts_provider: string
  llm_provider: string
}

export interface CreateUserRequest {
  email: string
  password: string
  name: string
}

export interface UpdateUserRequest {
  name?: string
  new_email?: string
  new_password?: string
  is_active?: boolean
}
