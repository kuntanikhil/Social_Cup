import { apiClient } from '@/src/api/apiClient';
import type { Membership, Profile } from '@/src/types/api';

export async function fetchProfile(): Promise<Profile> {
  const response = await apiClient.get<Profile>('/api/profile');
  return response.data;
}

export async function fetchMembership(): Promise<Membership> {
  const response = await apiClient.get<Membership>('/api/membership');
  return response.data;
}
