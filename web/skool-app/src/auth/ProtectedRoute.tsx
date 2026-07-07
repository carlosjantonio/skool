import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';
import type { Role } from '../api/types';

export function ProtectedRoute({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { session, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div className="center-screen">…</div>;
  if (!session) return <Navigate to="/login" state={{ from: location }} replace />;
  if (roles && !roles.some((r) => session.roles.includes(r))) {
    return <Navigate to="/nao-autorizado" replace />;
  }
  return <>{children}</>;
}
