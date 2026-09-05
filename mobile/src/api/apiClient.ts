import axios, {
  AxiosError,
  type InternalAxiosRequestConfig,
} from 'axios';

import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  storeTokens,
} from '@/src/auth/tokenStorage';
import type { AuthTokens } from '@/src/types/api';

const configuredUrl = process.env.EXPO_PUBLIC_API_URL?.trim();

export const API_BASE_URL = configuredUrl?.replace(/\/$/, '') ?? '';

export const apiClient = axios.create({
  baseURL: API_BASE_URL || undefined,
  timeout: 12_000,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
});

type RetryableRequest = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

let refreshPromise: Promise<AuthTokens> | null = null;
let authenticationFailureHandler: (() => void) | null = null;

export function setAuthenticationFailureHandler(
  handler: (() => void) | null,
): void {
  authenticationFailureHandler = handler;
}

apiClient.interceptors.request.use(async (config) => {
  if (!API_BASE_URL) {
    return Promise.reject(
      new Error('EXPO_PUBLIC_API_URL is not configured.'),
    );
  }

  const accessToken = await getAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequest | undefined;
    const isAuthEndpoint = originalRequest?.url?.startsWith('/api/auth/');

    if (
      error.response?.status !== 401 ||
      !originalRequest ||
      originalRequest._retry ||
      isAuthEndpoint
    ) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;
    try {
      const tokens = await refreshAccessToken();
      originalRequest.headers.Authorization = `Bearer ${tokens.accessToken}`;
      return apiClient(originalRequest);
    } catch (refreshError) {
      await clearTokens();
      authenticationFailureHandler?.();
      return Promise.reject(refreshError);
    }
  },
);

async function refreshAccessToken(): Promise<AuthTokens> {
  if (!refreshPromise) {
    refreshPromise = performRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function performRefresh(): Promise<AuthTokens> {
  const refreshToken = await getRefreshToken();
  if (!refreshToken || !API_BASE_URL) {
    throw new Error('Your session has expired. Please sign in again.');
  }

  const response = await axios.post<AuthTokens>(
    `${API_BASE_URL}/api/auth/refresh`,
    { refreshToken },
    { timeout: 12_000 },
  );
  await storeTokens(response.data);
  return response.data;
}
