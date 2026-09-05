import type { DeviceSession } from '../types'

const STORAGE_KEY = 'social-cup-cafe-device'

export function getDeviceSession(): DeviceSession | null {
  const stored = window.sessionStorage.getItem(STORAGE_KEY)
  if (!stored) return null

  try {
    const session = JSON.parse(stored) as Partial<DeviceSession>
    if (
      typeof session.cafeId !== 'number' ||
      typeof session.cafeName !== 'string' ||
      typeof session.deviceToken !== 'string' ||
      session.deviceToken.length === 0
    ) {
      clearDeviceSession()
      return null
    }
    if (session.expiresAt && Date.parse(session.expiresAt) <= Date.now()) {
      clearDeviceSession()
      return null
    }
    return session as DeviceSession
  } catch {
    clearDeviceSession()
    return null
  }
}

export function storeDeviceSession(session: DeviceSession): void {
  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearDeviceSession(): void {
  window.sessionStorage.removeItem(STORAGE_KEY)
}
