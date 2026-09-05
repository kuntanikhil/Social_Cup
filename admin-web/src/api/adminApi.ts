import { apiClient } from './apiClient'
import type { AdminCafe, AdminDrink, AdminRedemption, CafeWriteRequest, DrinkWriteRequest, Neighbourhood } from '../types'

export async function fetchCafes(): Promise<AdminCafe[]> {
  return (await apiClient.get<AdminCafe[]>('/api/admin/cafes')).data
}

export async function fetchNeighbourhoods(): Promise<Neighbourhood[]> {
  return (await apiClient.get<Neighbourhood[]>('/api/neighbourhoods')).data
}

export async function createCafe(request: CafeWriteRequest): Promise<AdminCafe> {
  const { active: _active, ...createRequest } = request
  return (await apiClient.post<AdminCafe>('/api/admin/cafes', createRequest)).data
}

export async function updateCafe(id: number, request: CafeWriteRequest): Promise<AdminCafe> {
  return (await apiClient.put<AdminCafe>(`/api/admin/cafes/${id}`, request)).data
}

export async function updateBaristaPin(cafeId: number, pin: string): Promise<{ cafeId: number; message: string }> {
  return (await apiClient.put(`/api/admin/cafes/${cafeId}/barista-pin`, { pin })).data
}

export async function fetchDrinks(cafeId: number): Promise<AdminDrink[]> {
  return (await apiClient.get<AdminDrink[]>(`/api/admin/cafes/${cafeId}/drinks`)).data
}

export async function createDrink(cafeId: number, request: DrinkWriteRequest): Promise<AdminDrink> {
  const { active: _active, ...createRequest } = request
  return (await apiClient.post<AdminDrink>(`/api/admin/cafes/${cafeId}/drinks`, createRequest)).data
}

export async function updateDrink(id: number, request: DrinkWriteRequest): Promise<AdminDrink> {
  return (await apiClient.put<AdminDrink>(`/api/admin/drinks/${id}`, request)).data
}

export async function fetchRedemptions(limit = 100): Promise<AdminRedemption[]> {
  return (await apiClient.get<AdminRedemption[]>('/api/admin/redemptions', { params: { limit } })).data
}
