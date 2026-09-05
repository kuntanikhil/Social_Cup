import { apiClient } from '@/src/api/apiClient';
import type {
  CreateRedemptionSessionResponse,
  RedemptionSession,
} from '@/src/types/api';

export async function createRedemptionSession(
  cafeId: number,
  drinkId: number,
): Promise<CreateRedemptionSessionResponse> {
  const response = await apiClient.post<CreateRedemptionSessionResponse>(
    `/api/cafes/${cafeId}/redemption-sessions`,
    { drinkId },
  );
  return response.data;
}

export async function fetchRedemptionSession(
  sessionId: number,
): Promise<RedemptionSession> {
  const response = await apiClient.get<RedemptionSession>(
    `/api/redemption-sessions/${sessionId}`,
  );
  return response.data;
}
