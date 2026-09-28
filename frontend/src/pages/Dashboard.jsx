import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, probe } from "../api";
import { ROLES, ROLE_LABEL, useAuth } from "../auth";
import { Badge, PageHead } from "../ui";

const SERVICES = [
  { id: "mgmt", label: "API Management", prefix: "/api", port: 8081 },
  { id: "gateway", label: "Smart Gateway", prefix: "/svc/gateway", port: 8080 },
  { id: "analytics", label: "Analytics", prefix: "/svc/analytics", port: 8082 },
];

function Stat({ label, value }) {
  return (
    <div className="stat">
      <span className="stat-value">{value ?? "—"}</span>
      <span className="stat-label">{label}</span>
    </div>
  );
}

export default function Dashboard() {
  const { user, hasRole } = useAuth();
  const [stats, setStats] = useState(null);
  const [health, setHealth] = useState({});
  const isAdmin = hasRole(ROLES.PLATFORM_ADMIN, ROLES.TENANT_ADMIN);

  const check = useCallback(async () => {
    const entries = await Promise.all(SERVICES.map(async (s) => [s.id, await probe(s.prefix)]));
    setHealth(Object.fromEntries(entries));
  }, []);

  useEffect(() => {
    check();
    const t = setInterval(check, 10000);
    return () => clearInterval(t);
  }, [check]);

  useEffect(() => {
    if (isAdmin) api("/stats").then(setStats).catch(() => {});
  }, [isAdmin]);

  return (
    <>
      <PageHead title={`Welcome, ${user.username}`} subtitle={`${ROLE_LABEL[user.role]}${user.organizationName ? ` · ${user.organizationName}` : ""}`} />

      {isAdmin && (
        <section className="stats">
          {user.role === ROLES.PLATFORM_ADMIN && <Stat label="Organizations" value={stats?.organizations} />}
          {user.role === ROLES.PLATFORM_ADMIN && <Stat label="Users" value={stats?.users} />}
          <Stat label="Routes" value={stats?.routes} />
          <Stat label="Active routes" value={stats?.activeRoutes} />
        </section>
      )}

      {isAdmin && stats && stats.routes === 0 && (
        <div className="callout">
          <strong>No routes yet.</strong> Create your first route to start proxying traffic through the gateway.{" "}
          <Link to="/routes">Go to Routes →</Link>
        </div>
      )}

      <h3 className="section-title">Service health</h3>
      <section className="health-grid">
        {SERVICES.map((s) => {
          const h = health[s.id];
          const tone = !h ? "muted" : h.up ? "ok" : "bad";
          const detail = h?.data?.dbStatus ? `database ${h.data.dbStatus}` : h?.data?.appStatus ? h.data.appStatus : h?.error ?? "checking…";
          return (
            <div key={s.id} className="card">
              <div className="card-row-top">
                <strong>{s.label}</strong>
                <Badge tone={tone}>{!h ? "checking" : h.up ? "up" : "down"}</Badge>
              </div>
              <p className="muted small">:{s.port} · {detail}</p>
              <p className="mono small">{h?.latency != null ? `${h.latency} ms` : "—"}</p>
            </div>
          );
        })}
      </section>
    </>
  );
}
