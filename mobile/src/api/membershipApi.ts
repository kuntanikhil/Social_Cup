import { apiClient } from '@/src/api/apiClient';
import type { Membership, StripeCheckoutResponse } from '@/src/types/api';

export async function getMembership(): Promise<Membership> {
  const response = await apiClient.get<Membership>('/api/membership');
  return response.data;
}

export async function createMembershipCheckout(): Promise<StripeCheckoutResponse> {
  const response = await apiClient.post<StripeCheckoutResponse>(
    '/api/membership/checkout',
  );
  return response.data;
}

export async function reconcileMembership(): Promise<Membership> {
  const response = await apiClient.post<Membership>(
    '/api/membership/reconcile',
  );
  return response.data;
}
