import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { api, session, adminSession } from './api.js';
import { BrandLoader } from './components/brand';

const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => session.get()?.user ?? null);
  const [ready, setReady] = useState(!session.get());

  useEffect(() => {
    if (!session.get()) return;
    api.get('/auth/me').then((r) => { setUser(r.data); const s = session.get(); if (s) session.set({ ...s, user: r.data }); })
      .catch(() => { session.clear(); setUser(null); }).finally(() => setReady(true));
  }, []);

  const acceptSession = useCallback((s) => { session.set(s); setUser(s.user); }, []);
  const login = useCallback(async (email, password) => { const { data } = await api.post('/auth/login', { email, password }); acceptSession(data); return data.user; }, [acceptSession]);
  const logout = useCallback(async () => {
    const s = session.get();
    try { await api.post('/auth/logout', s ? { refreshToken: s.refreshToken } : {}); } catch { /* already signed out */ }
    session.clear(); setUser(null);
  }, []);
  const can = useCallback((perm) => !perm || !!user?.permissions?.includes(perm), [user]);

  const value = useMemo(() => ({ user, ready, login, logout, can, acceptSession }), [user, ready, login, logout, can, acceptSession]);
  if (!ready) return <BrandLoader />;
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

/** UI guard only. Every API call is authorised again by the backend. */
export function RequireAuth({ perm, children }) {
  const { user, can } = useAuth();
  const loc = useLocation();
  if (!user) return <Navigate to="/login" state={{ from: loc.pathname }} replace />;
  if (perm && !can(perm)) return <Navigate to="/app" replace />;
  return children;
}

export function RequireAdmin({ children }) {
  const s = adminSession.get();
  if (!s?.accessToken) return <Navigate to="/admin" replace />;
  return children;
}

export function homeFor(user) { return user?.role === 'DRIVER' ? '/app' : '/app'; }
