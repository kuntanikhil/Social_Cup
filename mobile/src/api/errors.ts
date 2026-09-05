import axios from 'axios';

export function getApiErrorMessage(
  error: unknown,
  fallback = 'Something went wrong. Please try again.',
): string {
  if (error instanceof Error && error.message.includes('EXPO_PUBLIC_API_URL')) {
    return 'Set EXPO_PUBLIC_API_URL to your backend address and restart Expo.';
  }

  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return 'Unable to reach Social Cup. Check the backend address and your connection.';
    }

    if (error.response.status === 401) {
      return 'Your email or password is incorrect.';
    }
    if (error.response.status === 409) {
      return 'An account with this email already exists.';
    }

    const responseMessage = getSafeResponseMessage(error.response.data);
    if (responseMessage) {
      return responseMessage;
    }
  }

  return fallback;
}

export function getHttpStatus(error: unknown): number | null {
  return axios.isAxiosError(error) ? (error.response?.status ?? null) : null;
}

function getSafeResponseMessage(data: unknown): string | null {
  if (!data || typeof data !== 'object') {
    return null;
  }
  const candidate = data as Record<string, unknown>;
  for (const key of ['message', 'detail', 'error']) {
    const value = candidate[key];
    if (
      typeof value === 'string' &&
      value.length > 0 &&
      value.length <= 180 &&
      !value.includes('\n') &&
      !/exception|stack trace|org\.springframework|java\./i.test(value)
    ) {
      return value;
    }
  }
  return null;
}
