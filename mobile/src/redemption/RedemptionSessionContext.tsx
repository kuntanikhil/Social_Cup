import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';

import { useAuth } from '@/src/auth/AuthContext';
import type { CreateRedemptionSessionResponse } from '@/src/types/api';

type RedemptionSessionContextValue = {
  activeSession: CreateRedemptionSessionResponse | null;
  setActiveSession: (session: CreateRedemptionSessionResponse) => void;
  clearActiveSession: () => void;
};

const RedemptionSessionContext =
  createContext<RedemptionSessionContextValue | null>(null);

export function RedemptionSessionProvider({ children }: PropsWithChildren) {
  const { isAuthenticated, isLoading } = useAuth();
  const [activeSession, setSession] =
    useState<CreateRedemptionSessionResponse | null>(null);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      setSession(null);
    }
  }, [isAuthenticated, isLoading]);

  const setActiveSession = useCallback(
    (session: CreateRedemptionSessionResponse) => setSession(session),
    [],
  );
  const clearActiveSession = useCallback(() => setSession(null), []);

  const value = useMemo(
    () => ({ activeSession, setActiveSession, clearActiveSession }),
    [activeSession, clearActiveSession, setActiveSession],
  );

  return (
    <RedemptionSessionContext.Provider value={value}>
      {children}
    </RedemptionSessionContext.Provider>
  );
}

export function useRedemptionSession(): RedemptionSessionContextValue {
  const context = useContext(RedemptionSessionContext);
  if (!context) {
    throw new Error(
      'useRedemptionSession must be used inside RedemptionSessionProvider.',
    );
  }
  return context;
}
