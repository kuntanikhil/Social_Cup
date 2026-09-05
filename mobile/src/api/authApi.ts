import { apiClient } from '@/src/api/apiClient';
import type {
  AuthTokens,
  LoginRequest,
  Profile,
  RegisterRequest,
} from '@/src/types/api';

export async function loginRequest(
  request: LoginRequest,
): Promise<AuthTokens> {
  const response = await apiClient.post<AuthTokens>('/api/auth/login', request);
  return response.data;
}

export async function registerRequest(
  request: RegisterRequest,
): Promise<Profile> {
  const response = await apiClient.post<Profile>('/api/auth/register', request);
  return response.data;
}

export async function getProfile(): Promise<Profile> {
  const response = await apiClient.get<Profile>('/api/profile');
  return response.data;
}

export async function logoutRequest(refreshToken: string): Promise<void> {
  await apiClient.post('/api/auth/logout', { refreshToken });
}
