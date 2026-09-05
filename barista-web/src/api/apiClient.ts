import axios from 'axios'
import { clearDeviceSession, getDeviceSession } from '../auth/deviceSession'

export const DEVICE_UNAUTHORIZED_EVENT = 'socialcup:device-unauthorized'

export const apiClient = axios.create({
  baseURL: import.meta.env.DEV ? '' : import.meta.env.VITE_API_URL,
  headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
  timeout: 12_000,
})

apiClient.interceptors.request.use((config) => {
  const session = getDeviceSession()
  if (session && config.url !== '/api/barista/device/authenticate') {
    config.headers['X-Cafe-Device-Token'] = session.deviceToken
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const data = error.response?.data
      const reason = isRecord(data) ? data.reason : undefined
      if (error.response?.status === 401 && reason === 'DEVICE_UNAUTHORIZED') {
        clearDeviceSession()
        window.dispatchEvent(new Event(DEVICE_UNAUTHORIZED_EVENT))
      }
    }
    return Promise.reject(error)
  },
)

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}
