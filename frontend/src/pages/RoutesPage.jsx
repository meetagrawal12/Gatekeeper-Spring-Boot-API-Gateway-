import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../api";
import { ROLES, useAuth } from "../auth";
import { Badge, Empty, ErrorBanner, Modal, PageHead, fmtDate, useToast } from "../ui";

const METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"];

const blank = {
  routeId: "", name: "", pathPattern: "/", targetBaseUrl: "https://", targetPathPrefix: "/",
  requestsPerMinute: "", responseTimeoutMs: 5000, idempotencyEnabled: false,
  allowedMethods: ["GET"], headerRules: [],
};

function RouteForm({ initial, editing, onSubmit, onClose }) {
  const [f, setF] = useState(initial);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (k, v) => setF((p) => ({ ...p, [k]: v }));

  const toggleMethod = (m) =>
    set("allowedMethods", f.allowedMethods.includes(m) ? f.allowedMethods.filter((x) => x !== m) : [...f.allowedMethods, m]);
  const setRule = (i, patch) => set("headerRules", f.headerRules.map((r, idx) => (idx === i ? { ...r, ...patch } : r)));

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onSubmit({
        ...f,
        requestsPerMinute: f.requestsPerMinute === "" ? null : Number(f.requestsPerMinute),
        responseTimeoutMs: Number(f.responseTimeoutMs),
      });
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  return (
    <Modal title={editing ? `Edit ${initial.routeId}` : "New route"} onClose={onClose} wide>
      <form onSubmit={submit} className="form-grid">
        <ErrorBanner error={error} />
        <label>Route ID
          <input value={f.routeId} onChange={(e) => set("routeId", e.target.value.toLowerCase())} disabled={editing} required placeholder="orders-api" />
          <span className="hint">Lowercase letters, digits and dashes. Cannot be changed later.</span>
        </label>
        <label>Name<input value={f.name} onChange={(e) => set("name", e.target.value)} required /></label>
        <label>Path pattern<input value={f.pathPattern} onChange={(e) => set("pathPattern", e.target.value)} required placeholder="/orders/**" /></label>
        <label>Target base URL<input value={f.targetBaseUrl} onChange={(e) => set("targetBaseUrl", e.target.value)} required placeholder="https://api.example.com" /></label>
        <label>Target path prefix<input value={f.targetPathPrefix} onChange={(e) => set("targetPathPrefix", e.target.value)} /></label>
        <div className="two-col">
          <label>Requests / minute<input type="number" min="1" value={f.requestsPerMinute} onChange={(e) => set("requestsPerMinute", e.target.value)} placeholder="unlimited" /></label>
          <label>Timeout (ms)<input type="number" min="100" max="120000" value={f.responseTimeoutMs} onChange={(e) => set("responseTimeoutMs", e.target.value)} required /></label>
        </div>

        <fieldset>
          <legend>Allowed methods</legend>
          <div className="chips">
            {METHODS.map((m) => (
              <button type="button" key={m} className={`chip${f.allowedMethods.includes(m) ? " on" : ""}`} onClick={() => toggleMethod(m)}>{m}</button>
            ))}
          </div>
        </fieldset>

        <label className="check"><input type="checkbox" checked={f.idempotencyEnabled} onChange={(e) => set("idempotencyEnabled", e.target.checked)} /> Enable idempotency keys</label>

        <fieldset>
          <legend>Header rules</legend>
          {f.headerRules.map((r, i) => (
            <div className="rule-row" key={i}>
              <select value={r.direction} onChange={(e) => setRule(i, { direction: e.target.value })}>
                <option>REQUEST</option><option>RESPONSE</option>
              </select>
              <select value={r.action} onChange={(e) => setRule(i, { action: e.target.value })}>
                <option>SET</option><option>ADD</option><option>REMOVE</option>
              </select>
              <input placeholder="Header" value={r.headerName} onChange={(e) => setRule(i, { headerName: e.target.value })} required />
              <input placeholder="Value" value={r.headerValue ?? ""} disabled={r.action === "REMOVE"} onChange={(e) => setRule(i, { headerValue: e.target.value })} />
              <button type="button" className="icon-btn" onClick={() => set("headerRules", f.headerRules.filter((_, idx) => idx !== i))} aria-label="Remove rule">✕</button>
            </div>
          ))}
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => set("headerRules", [...f.headerRules, { direction: "REQUEST", action: "SET", headerName: "", headerValue: "" }])}>+ Add header rule</button>
        </fieldset>

        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? "Saving…" : editing ? "Save changes" : "Create route"}</button>
        </div>
      </form>
    </Modal>
  );
}

function History({ route, orgParam, onClose }) {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState(null);
  useEffect(() => {
    api(`/routes/${route.routeId}/versions`, { params: orgParam }).then(setRows).catch(setError);
  }, [route.routeId, orgParam]);
  const pretty = (s) => { try { return JSON.stringify(JSON.parse(s), null, 2); } catch { return s; } };

  return (
    <Modal title={`History · ${route.routeId}`} onClose={onClose} wide>
      <ErrorBanner error={error} />
      {!rows && !error && <p className="muted">Loading…</p>}
      {rows?.map((v) => (
        <details key={v.versionNumber} className="version">
          <summary><strong>v{v.versionNumber}</strong> <Badge>{v.changeType}</Badge> <span className="muted small">{fmtDate(v.createdAt)}</span></summary>
          <pre className="code">{pretty(v.configurationSnapshot)}</pre>
        </details>
      ))}
    </Modal>
  );
}

export default function RoutesPage() {
  const { user } = useAuth();
  const toast = useToast();
  const isPlatform = user.role === ROLES.PLATFORM_ADMIN;
  const [routes, setRoutes] = useState(null);
  const [orgs, setOrgs] = useState([]);
  const [orgId, setOrgId] = useState("");
  const [error, setError] = useState(null);
  const [form, setForm] = useState(null); // { initial, editing }
  const [history, setHistory] = useState(null);

  const orgName = useMemo(() => Object.fromEntries(orgs.map((o) => [o.id, o.name])), [orgs]);
  // Platform admins act on the organization that owns each route; tenants are pinned server-side.
  const paramFor = (orgIdValue) => (isPlatform ? { organizationId: orgIdValue } : undefined);

  const load = useCallback(() => {
    api("/routes", { params: isPlatform && orgId ? { organizationId: orgId } : undefined })
      .then((r) => { setRoutes(r); setError(null); })
      .catch(setError);
  }, [isPlatform, orgId]);

  useEffect(() => {
    if (isPlatform) api("/admin/organizations").then(setOrgs).catch(setError);
  }, [isPlatform]);
  useEffect(load, [load]);

  const save = async (payload) => {
    if (form.editing) {
      await api(`/routes/${payload.routeId}`, { method: "PUT", body: payload, params: paramFor(form.orgId) });
      toast("Route updated");
    } else {
      await api("/routes", { method: "POST", body: payload, params: paramFor(orgId) });
      toast("Route created (inactive until you activate it)");
    }
    setForm(null);
    load();
  };

  const act = async (fn, okMsg) => {
    try { await fn(); toast(okMsg); load(); } catch (e) { toast(e.message, "bad"); }
  };

  const toggle = (r) =>
    act(() => api(`/routes/${r.routeId}/${r.active ? "deactivate" : "activate"}`, { method: "POST", params: paramFor(r.organizationId) }),
      r.active ? "Route deactivated" : "Route activated");

  const remove = (r) => {
    if (!window.confirm(`Delete route "${r.routeId}"? This cannot be undone.`)) return;
    act(() => api(`/routes/${r.routeId}`, { method: "DELETE", params: paramFor(r.organizationId) }), "Route deleted");
  };

  const canCreate = !isPlatform || Boolean(orgId);

  return (
    <>
      <PageHead title="Routes" subtitle="Define how requests are matched and forwarded by the gateway.">
        {isPlatform && (
          <select value={orgId} onChange={(e) => setOrgId(e.target.value)} aria-label="Organization">
            <option value="">All organizations</option>
            {orgs.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
          </select>
        )}
        <button className="btn btn-primary" disabled={!canCreate} title={canCreate ? "" : "Pick an organization first"}
          onClick={() => setForm({ initial: blank, editing: false })}>+ New route</button>
      </PageHead>

      <ErrorBanner error={error} />
      {!routes && !error && <p className="muted">Loading…</p>}
      {routes && routes.length === 0 && <Empty title="No routes yet" hint={canCreate ? "Create one to get started." : "Choose an organization to create routes for."} />}

      {routes && routes.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Route</th>{isPlatform && <th>Organization</th>}<th>Path → Target</th><th>Methods</th><th>Rate</th><th>Ver</th><th>Status</th><th />
              </tr>
            </thead>
            <tbody>
              {routes.map((r) => (
                <tr key={r.id}>
                  <td><strong>{r.name}</strong><div className="mono small muted">{r.routeId}</div></td>
                  {isPlatform && <td>{orgName[r.organizationId] ?? r.organizationId}</td>}
                  <td><div className="mono small">{r.pathPattern}</div><div className="mono small muted">{r.targetBaseUrl}{r.targetPathPrefix !== "/" ? r.targetPathPrefix : ""}</div></td>
                  <td>{r.allowedMethods.join(" ")}</td>
                  <td>{r.requestsPerMinute ? `${r.requestsPerMinute}/min` : "∞"}</td>
                  <td className="mono">v{r.configurationVersion}</td>
                  <td>
                    <button className={`switch${r.active ? " on" : ""}`} onClick={() => toggle(r)} aria-label={r.active ? "Deactivate" : "Activate"} title={r.active ? "Active — click to deactivate" : "Inactive — click to activate"}><span /></button>
                  </td>
                  <td className="row-actions">
                    <button className="btn btn-ghost btn-sm" onClick={() => setForm({
                      editing: true, orgId: r.organizationId,
                      initial: { ...r, requestsPerMinute: r.requestsPerMinute ?? "", headerRules: r.headerRules.map((h) => ({ ...h, headerValue: h.headerValue ?? "" })) },
                    })}>Edit</button>
                    <button className="btn btn-ghost btn-sm" onClick={() => setHistory(r)}>History</button>
                    <button className="btn btn-danger btn-sm" onClick={() => remove(r)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {form && <RouteForm initial={form.initial} editing={form.editing} onSubmit={save} onClose={() => setForm(null)} />}
      {history && <History route={history} orgParam={paramFor(history.organizationId)} onClose={() => setHistory(null)} />}
    </>
  );
}
