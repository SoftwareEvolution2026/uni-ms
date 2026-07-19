import { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

export function ProtectedRoute({
  children,
  role,
}: {
  children: ReactNode;
  role?: string | string[];
}) {
  const { user, loading } = useAuth();

  if (loading) return <div className="center">Loading…</div>;
  if (!user) return <Navigate to="/login" replace />;

  if (role) {
    const allowedRoles = Array.isArray(role) ? role : [role];
    const isAuthorized = user.roles.some((userRole) => allowedRoles.includes(userRole));
    if (!isAuthorized) {
      return <Navigate to="/dashboard" replace />;
    }
  }

  return <>{children}</>;
}
