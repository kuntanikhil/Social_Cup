import { apiClient } from '@/src/api/apiClient';
import type { DiscoverResponse, Neighbourhood } from '@/src/types/api';

export type DiscoverQuery = {
  search?: string;
  neighbourhoodId?: number;
  latitude?: number;
  longitude?: number;
};

export async function fetchDiscover(
  query: DiscoverQuery = {},
): Promise<DiscoverResponse> {
  const response = await apiClient.get<DiscoverResponse>('/api/discover', {
    params: query,
  });
  return response.data;
}

export async function fetchNeighbourhoods(): Promise<Neighbourhood[]> {
  const response = await apiClient.get<Neighbourhood[]>('/api/neighbourhoods');
  return response.data;
}
