import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../api";
import { ROLES, useAuth } from "../auth";
import { Badge, Empty, ErrorBanner, PageHead, fmtDate } from "../ui";

export default function Audit() {
  const { user } = useAuth();
  const [rows, setRows] = useState(null);
  const [error, setError] = useState(null);
  const [q, setQ] = useState("");

  const load = useCallback(() => api("/audit").then((r) => { setRows(r); setError(null); }).catch(setError), []);
  useEffect(() => { load(); }, [load]);

  const shown = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!rows || !needle) return rows;
    return rows.filter((r) => [r.actor, r.action, r.resourceType, r.resourceId, r.details].some((v) => (v ?? "").toLowerCase().includes(needle)));
  }, [rows, q]);

  return (
    <>
      <PageHead title="Audit log" subtitle={user.role === ROLES.PLATFORM_ADMIN ? "Latest 200 control-plane events across the platform." : "Latest 200 events for your organization."}>
        <input className="search" placeholder="Filter…" value={q} onChange={(e) => setQ(e.target.value)} />
        <button className="btn btn-ghost" onClick={load}>Refresh</button>
      </PageHead>
      <ErrorBanner error={error} />
      {shown && shown.length === 0 && <Empty title="No matching events" />}
      {shown && shown.length > 0 && (
        <div className="table-wrap"><table>
          <thead><tr><th>Time</th><th>Actor</th><th>Action</th><th>Resource</th><th>Result</th><th>Details</th></tr></thead>
          <tbody>{shown.map((r) => (
            <tr key={r.id}>
              <td className="small muted nowrap">{fmtDate(r.createdAt)}</td>
              <td>{r.actor}</td>
              <td className="mono small">{r.action}</td>
              <td className="small">{r.resourceType} <span className="mono muted">{r.resourceId}</span></td>
              <td><Badge tone={r.result === "SUCCESS" ? "ok" : "bad"}>{r.result.toLowerCase()}</Badge></td>
              <td className="small muted">{r.details ?? ""}</td>
            </tr>))}</tbody>
        </table></div>
      )}
    </>
  );
}
