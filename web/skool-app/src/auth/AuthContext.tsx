import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { loadPersistedRefresh, onSessionEnd } from '../api/client';
import * as authApi from '../api/auth';
import type { Role, TokenResponse } from '../api/types';

interface Session {
  userId: string;
  tenantId: string;
  email: string;
  fullName: string;
  roles: Role[];
}

interface AuthContextValue {
  session: Session | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (role: Role) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function toSession(t: TokenResponse): Session {
  return {
    userId: t.userId,
    tenantId: t.tenantId,
    email: t.email,
    fullName: t.fullName,
    roles: t.roles,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = loadPersistedRefresh();
    if (!stored) {
      setLoading(false);
      return;
    }
    authApi
      .refresh(stored)
      .then((t) => setSession(toSession(t)))
      .catch(() => setSession(null))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    onSessionEnd(() => setSession(null));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const t = await authApi.login(email, password);
    setSession(toSession(t));
  }, []);

  const logout = useCallback(async () => {
    const stored = loadPersistedRefresh();
    if (stored) await authApi.logout(stored);
    setSession(null);
  }, []);

  const hasRole = useCallback((role: Role) => session?.roles.includes(role) ?? false, [session]);

  const value = useMemo(() => ({ session, loading, login, logout, hasRole }), [session, loading, login, logout, hasRole]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
