import { useCallback, useEffect, useState } from "react";
import { api } from "../api";
import { ROLE_LABEL, useAuth } from "../auth";
import { Badge, Empty, ErrorBanner, PageHead, fmtDate, useToast } from "../ui";

export default function Users() {
  const { user: me } = useAuth();
  const toast = useToast();
  const [rows, setRows] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(() => api("/admin/users").then((r) => { setRows(r); setError(null); }).catch(setError), []);
  useEffect(() => { load(); }, [load]);

  const flip = async (u) => {
    try { await api(`/admin/users/${u.id}/enabled`, { method: "PATCH", body: { enabled: !u.enabled } }); toast(`${u.username} ${u.enabled ? "disabled" : "enabled"}`); load(); }
    catch (e) { toast(e.message, "bad"); }
  };

  return (
    <>
      <PageHead title="Users" subtitle="Every account that can sign in to the control plane." />
      <ErrorBanner error={error} />
      {rows && rows.length === 0 && <Empty title="No users" />}
      {rows && rows.length > 0 && (
        <div className="table-wrap"><table>
          <thead><tr><th>Username</th><th>Role</th><th>Organization</th><th>Status</th><th>Created</th><th /></tr></thead>
          <tbody>{rows.map((u) => (
            <tr key={u.id}>
              <td><strong>{u.username}</strong>{u.id === me.id && <span className="muted small"> (you)</span>}</td>
              <td>{ROLE_LABEL[u.role] ?? u.role}</td>
              <td>{u.organizationName ?? <span className="muted">platform</span>}</td>
              <td><Badge tone={u.enabled ? "ok" : "bad"}>{u.enabled ? "enabled" : "disabled"}</Badge></td>
              <td className="small muted">{fmtDate(u.createdAt)}</td>
              <td className="row-actions"><button className="btn btn-ghost btn-sm" disabled={u.id === me.id} onClick={() => flip(u)}>{u.enabled ? "Disable" : "Enable"}</button></td>
            </tr>))}</tbody>
        </table></div>
      )}
    </>
  );
}
