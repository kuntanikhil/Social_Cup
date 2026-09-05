import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';

import {
  getProfile,
  loginRequest,
  logoutRequest,
  registerRequest,
} from '@/src/api/authApi';
import { setAuthenticationFailureHandler } from '@/src/api/apiClient';
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  storeTokens,
} from '@/src/auth/tokenStorage';
import type {
  LoginRequest,
  Profile,
  RegisterRequest,
} from '@/src/types/api';

type AuthContextValue = {
  user: Profile | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<Profile>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<Profile | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const clearAuthentication = useCallback(() => {
    setUser(null);
  }, []);

  useEffect(() => {
    setAuthenticationFailureHandler(clearAuthentication);
    return () => setAuthenticationFailureHandler(null);
  }, [clearAuthentication]);

  useEffect(() => {
    let active = true;

    async function restoreSession() {
      try {
        const accessToken = await getAccessToken();
        if (!accessToken) {
          await clearTokens();
          return;
        }

        const profile = await getProfile();
        if (active) {
          setUser(profile);
        }
      } catch {
        await clearTokens();
        if (active) {
          setUser(null);
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    void restoreSession();
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(async (request: LoginRequest) => {
    const tokens = await loginRequest(request);
    await storeTokens(tokens);
    try {
      setUser(await getProfile());
    } catch (error) {
      await clearTokens();
      throw error;
    }
  }, []);

  const register = useCallback((request: RegisterRequest) => {
    return registerRequest(request);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = await getRefreshToken();
    try {
      if (refreshToken) {
        await logoutRequest(refreshToken);
      }
    } finally {
      await clearTokens();
      setUser(null);
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      isAuthenticated: user !== null,
      login,
      register,
      logout,
    }),
    [isLoading, login, logout, register, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider.');
  }
  return context;
}
