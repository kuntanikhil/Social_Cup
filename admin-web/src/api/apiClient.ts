import axios from 'axios'
import { clearAccessToken, getAccessToken } from '../auth/tokenStorage'

export const ADMIN_UNAUTHORIZED_EVENT = 'socialcup:admin-unauthorized'
export const ADMIN_FORBIDDEN_EVENT = 'socialcup:admin-forbidden'

export const apiClient = axios.create({
  baseURL: import.meta.env.DEV ? '' : import.meta.env.VITE_API_URL,
  headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
  timeout: 12_000,
})

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token && config.url !== '/api/auth/login') {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      error.config?.url !== '/api/auth/login'
    ) {
      clearAccessToken()
      window.dispatchEvent(new Event(ADMIN_UNAUTHORIZED_EVENT))
    }
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 403
    ) {
      window.dispatchEvent(new Event(ADMIN_FORBIDDEN_EVENT))
    }
    return Promise.reject(error)
  },
)

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError(error)) return fallback
  if (!error.response || error.response.status >= 500) {
    return 'Unable to reach Social Cup. Check the backend and try again.'
  }
  if (error.response.status === 401) return 'Your session has expired. Sign in again.'
  if (error.response.status === 403) return 'Administrator access is required for this action.'

  const data = error.response.data
  if (typeof data === 'object' && data !== null) {
    for (const key of ['detail', 'message']) {
      const value = (data as Record<string, unknown>)[key]
      if (
        typeof value === 'string' &&
        value.length <= 180 &&
        !/exception|stack trace|org\.springframework|java\./i.test(value)
      ) return value
    }
  }
  return fallback
}
