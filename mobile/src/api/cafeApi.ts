import { apiClient } from '@/src/api/apiClient';
import type { CafeDetail } from '@/src/types/api';

export async function fetchCafe(id: number): Promise<CafeDetail> {
  const response = await apiClient.get<CafeDetail>(`/api/cafes/${id}`);
  return response.data;
}
