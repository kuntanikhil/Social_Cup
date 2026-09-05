import { createContext, useCallback, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { fetchProfile, login as loginRequest } from '../api/authApi'
import { ADMIN_FORBIDDEN_EVENT, ADMIN_UNAUTHORIZED_EVENT } from '../api/apiClient'
import { clearAccessToken, getAccessToken, storeAccessToken } from './tokenStorage'
import type { AdminProfile } from '../types'

export const ADMIN_ACCESS_MESSAGE = 'This account does not have administrator access.'

type AuthContextValue = {
  user: AdminProfile | null
  isLoading: boolean
  isAuthenticated: boolean
  authMessage: string | null
  authorizationMessage: string | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  clearAuthMessage: () => void
  clearAuthorizationMessage: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<AdminProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [authMessage, setAuthMessage] = useState<string | null>(null)
  const [authorizationMessage, setAuthorizationMessage] = useState<string | null>(null)

  const clearSession = useCallback(() => {
    clearAccessToken()
    setUser(null)
  }, [])

  useEffect(() => {
    let active = true

    const restoreSession = async () => {
      if (!getAccessToken()) {
        if (active) setIsLoading(false)
        return
      }

      try {
        const profile = await fetchProfile()
        if (!active) return
        if (profile.role !== 'ADMIN') {
          clearSession()
          setAuthMessage(ADMIN_ACCESS_MESSAGE)
          return
        }
        setUser(profile)
      } catch {
        if (active) clearSession()
      } finally {
        if (active) setIsLoading(false)
      }
    }

    void restoreSession()
    return () => { active = false }
  }, [clearSession])

  useEffect(() => {
    const unauthorized = () => {
      clearSession()
      setAuthMessage('Your session has expired. Sign in again.')
    }
    const forbidden = () => setAuthorizationMessage('Administrator access is required for this action.')
    window.addEventListener(ADMIN_UNAUTHORIZED_EVENT, unauthorized)
    window.addEventListener(ADMIN_FORBIDDEN_EVENT, forbidden)
    return () => {
      window.removeEventListener(ADMIN_UNAUTHORIZED_EVENT, unauthorized)
      window.removeEventListener(ADMIN_FORBIDDEN_EVENT, forbidden)
    }
  }, [clearSession])

  const login = useCallback(async (email: string, password: string) => {
    setAuthMessage(null)
    setAuthorizationMessage(null)
    const response = await loginRequest(email, password)
    storeAccessToken(response.accessToken)
    try {
      const profile = await fetchProfile()
      if (profile.role !== 'ADMIN') {
        clearSession()
        setAuthMessage(ADMIN_ACCESS_MESSAGE)
        throw new Error(ADMIN_ACCESS_MESSAGE)
      }
      setUser(profile)
    } catch (error) {
      if (error instanceof Error && error.message === ADMIN_ACCESS_MESSAGE) throw error
      clearSession()
      throw error
    }
  }, [clearSession])

  const logout = useCallback(() => {
    clearSession()
    setAuthMessage(null)
    setAuthorizationMessage(null)
  }, [clearSession])

  const clearAuthMessage = useCallback(() => setAuthMessage(null), [])
  const clearAuthorizationMessage = useCallback(() => setAuthorizationMessage(null), [])

  const value = useMemo(() => ({
    user,
    isLoading,
    isAuthenticated: user?.role === 'ADMIN',
    authMessage,
    authorizationMessage,
    login,
    logout,
    clearAuthMessage,
    clearAuthorizationMessage,
  }), [user, isLoading, authMessage, authorizationMessage, login, logout, clearAuthMessage, clearAuthorizationMessage])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// oxlint-disable-next-line react/only-export-components -- Provider and hook intentionally share one private context.
export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside AuthProvider')
  return value
}
