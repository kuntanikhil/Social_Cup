import { apiClient } from '@/src/api/apiClient';
import type {
  DrinkDiaryEntry,
  RatingRequest,
  RatingResponse,
  RatingSummary,
} from '@/src/types/api';

export async function setDrinkRating(
  drinkId: number,
  request: RatingRequest,
): Promise<RatingResponse> {
  const response = await apiClient.put<RatingResponse>(
    `/api/drinks/${drinkId}/rating`,
    request,
  );
  return response.data;
}

export async function getMyDrinkRating(
  drinkId: number,
): Promise<RatingResponse> {
  const response = await apiClient.get<RatingResponse>(
    `/api/drinks/${drinkId}/my-rating`,
  );
  return response.data;
}

export async function getDrinkDiary(): Promise<DrinkDiaryEntry[]> {
  const response = await apiClient.get<DrinkDiaryEntry[]>(
    '/api/profile/drink-diary',
  );
  return response.data;
}

export async function getDrinkRatingSummary(
  drinkId: number,
): Promise<RatingSummary> {
  const response = await apiClient.get<RatingSummary>(
    `/api/drinks/${drinkId}/rating-summary`,
  );
  return response.data;
}
