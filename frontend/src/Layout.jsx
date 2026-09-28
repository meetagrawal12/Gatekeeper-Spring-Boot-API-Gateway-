import { NavLink, Outlet } from "react-router-dom";
import { ROLES, ROLE_LABEL, useAuth } from "./auth";

const NAV = [
  { to: "/", label: "Dashboard", end: true, roles: null },
  { to: "/routes", label: "Routes", roles: [ROLES.PLATFORM_ADMIN, ROLES.TENANT_ADMIN] },
  { to: "/audit", label: "Audit log", roles: [ROLES.PLATFORM_ADMIN, ROLES.TENANT_ADMIN] },
  { to: "/organizations", label: "Organizations", roles: [ROLES.PLATFORM_ADMIN] },
  { to: "/users", label: "Users", roles: [ROLES.PLATFORM_ADMIN] },
  { to: "/gateway-config", label: "Active config", roles: [ROLES.GATEWAY_SERVICE] },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const items = NAV.filter((n) => !n.roles || n.roles.includes(user.role));

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">
            <svg viewBox="0 0 24 24" fill="none"><path d="M12 2 4 5v6c0 5 3.4 8.7 8 10 4.6-1.3 8-5 8-10V5l-8-3Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" /><path d="m9 12 2 2 4-4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" /></svg>
          </span>
          Gatekeeper
        </div>
        <nav>
          {items.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="who">
            <span className="who-name">{user.username}</span>
            <span className="who-meta">{ROLE_LABEL[user.role]}{user.organizationName ? ` · ${user.organizationName}` : ""}</span>
          </div>
          <button className="btn btn-ghost btn-block" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
