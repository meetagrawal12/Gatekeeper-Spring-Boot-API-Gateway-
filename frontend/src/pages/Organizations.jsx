import { useCallback, useEffect, useState } from "react";
import { api } from "../api";
import { Badge, Empty, ErrorBanner, PageHead, fmtDate, useToast } from "../ui";

export default function Organizations() {
  const toast = useToast();
  const [rows, setRows] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(() => api("/admin/organizations").then((r) => { setRows(r); setError(null); }).catch(setError), []);
  useEffect(() => { load(); }, [load]);

  const flip = async (o) => {
    const status = o.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE";
    if (status === "SUSPENDED" && !window.confirm(`Suspend "${o.name}"? Its users will be unable to sign in and its routes stop being served.`)) return;
    try { await api(`/admin/organizations/${o.id}/status`, { method: "PATCH", body: { status } }); toast(`${o.name} is now ${status.toLowerCase()}`); load(); }
    catch (e) { toast(e.message, "bad"); }
  };

  return (
    <>
      <PageHead title="Organizations" subtitle="Tenants registered on the platform." />
      <ErrorBanner error={error} />
      {rows && rows.length === 0 && <Empty title="No organizations yet" />}
      {rows && rows.length > 0 && (
        <div className="table-wrap"><table>
          <thead><tr><th>Name</th><th>Slug</th><th>Status</th><th>Created</th><th /></tr></thead>
          <tbody>{rows.map((o) => (
            <tr key={o.id}>
              <td><strong>{o.name}</strong></td>
              <td className="mono small">{o.slug}</td>
              <td><Badge tone={o.status === "ACTIVE" ? "ok" : "bad"}>{o.status.toLowerCase()}</Badge></td>
              <td className="small muted">{fmtDate(o.createdAt)}</td>
              <td className="row-actions"><button className={`btn btn-sm ${o.status === "ACTIVE" ? "btn-danger" : "btn-ghost"}`} onClick={() => flip(o)}>{o.status === "ACTIVE" ? "Suspend" : "Reactivate"}</button></td>
            </tr>))}</tbody>
        </table></div>
      )}
    </>
  );
}
