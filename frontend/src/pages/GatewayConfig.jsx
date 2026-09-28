import { useEffect, useState } from "react";
import { api } from "../api";
import { Empty, ErrorBanner, PageHead } from "../ui";

/** Read-only view of what the GATEWAY_SERVICE account receives from /internal/routes. */
export default function GatewayConfig() {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState(null);
  useEffect(() => { api("/internal/routes").then(setRows).catch(setError); }, []);

  return (
    <>
      <PageHead title="Active gateway configuration" subtitle="Routes the gateway should currently enforce (active routes of active organizations)." />
      <ErrorBanner error={error} />
      {rows && rows.length === 0 && <Empty title="No active routes" />}
      {rows && rows.length > 0 && <pre className="code">{JSON.stringify(rows, null, 2)}</pre>}
    </>
  );
}
