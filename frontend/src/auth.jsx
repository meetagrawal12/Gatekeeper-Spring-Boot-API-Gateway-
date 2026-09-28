import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, onUnauthorized, tokenStore } from "./api";

const AuthContext = createContext(null);

export const ROLES = {
  PLATFORM_ADMIN: "PLATFORM_ADMIN",
  TENANT_ADMIN: "TENANT_ADMIN",
  GATEWAY_SERVICE: "GATEWAY_SERVICE",
};

export const ROLE_LABEL = {
  PLATFORM_ADMIN: "Platform admin",
  TENANT_ADMIN: "Tenant admin",
  GATEWAY_SERVICE: "Gateway service",
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [booting, setBooting] = useState(Boolean(tokenStore.get()));

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  useEffect(() => {
    onUnauthorized(logout);
  }, [logout]);

  // Restore the session on page refresh.
  useEffect(() => {
    if (!tokenStore.get()) return;
    api("/auth/me")
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setBooting(false));
  }, []);

  const finish = (res) => {
    tokenStore.set(res.accessToken);
    setUser(res.user);
    return res.user;
  };

  const login = (username, password) =>
    api("/auth/login", { method: "POST", auth: false, body: { username, password } }).then(finish);

  const register = (organizationName, username, password) =>
    api("/auth/register", { method: "POST", auth: false, body: { organizationName, username, password } }).then(finish);

  const value = useMemo(
    () => ({ user, booting, login, register, logout, hasRole: (...roles) => user && roles.includes(user.role) }),
    [user, booting, logout]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
