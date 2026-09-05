import { apiClient } from './apiClient'
import type { AdminProfile, AuthResponse } from '../types'

export async function login(email: string, password: string): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>('/api/auth/login', { email, password })
  return response.data
}

export async function fetchProfile(): Promise<AdminProfile> {
  const response = await apiClient.get<AdminProfile>('/api/profile')
  return response.data
}
