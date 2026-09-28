import { Navigate, Route, Routes } from "react-router-dom";
import { ROLES, useAuth } from "./auth";
import Layout from "./Layout";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import RoutesPage from "./pages/RoutesPage";
import Organizations from "./pages/Organizations";
import Users from "./pages/Users";
import Audit from "./pages/Audit";
import GatewayConfig from "./pages/GatewayConfig";

function Protected({ roles, children }) {
  const { user, booting } = useAuth();
  if (booting) return <div className="boot">Loading…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

function GuestOnly({ children }) {
  const { user, booting } = useAuth();
  if (booting) return <div className="boot">Loading…</div>;
  return user ? <Navigate to="/" replace /> : children;
}

const { PLATFORM_ADMIN, TENANT_ADMIN, GATEWAY_SERVICE } = ROLES;

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<GuestOnly><Login /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><Register /></GuestOnly>} />
      <Route element={<Protected><Layout /></Protected>}>
        <Route index element={<Dashboard />} />
        <Route path="routes" element={<Protected roles={[PLATFORM_ADMIN, TENANT_ADMIN]}><RoutesPage /></Protected>} />
        <Route path="audit" element={<Protected roles={[PLATFORM_ADMIN, TENANT_ADMIN]}><Audit /></Protected>} />
        <Route path="organizations" element={<Protected roles={[PLATFORM_ADMIN]}><Organizations /></Protected>} />
        <Route path="users" element={<Protected roles={[PLATFORM_ADMIN]}><Users /></Protected>} />
        <Route path="gateway-config" element={<Protected roles={[GATEWAY_SERVICE]}><GatewayConfig /></Protected>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
