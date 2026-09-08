import { apiClient } from '@/src/api/apiClient';
import type { Profile } from '@/src/types/api';

export async function fetchProfile(): Promise<Profile> {
  const response = await apiClient.get<Profile>('/api/profile');
  return response.data;
}
