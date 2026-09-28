import { createContext, useCallback, useContext, useEffect, useState } from "react";

export function Badge({ tone = "muted", children }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}

export function Modal({ title, onClose, wide, children }) {
  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);
  return (
    <div className="overlay" onMouseDown={onClose}>
      <div className={`modal${wide ? " modal-wide" : ""}`} role="dialog" aria-modal="true" onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="icon-btn" onClick={onClose} aria-label="Close">✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function ErrorBanner({ error }) {
  if (!error) return null;
  return (
    <div className="banner-error" role="alert">
      {error.message ?? String(error)}
      {error.details?.length > 0 && (
        <ul>{error.details.map((d) => <li key={d}>{d}</li>)}</ul>
      )}
    </div>
  );
}

export function Empty({ title, hint, children }) {
  return (
    <div className="empty">
      <p className="empty-title">{title}</p>
      {hint && <p className="empty-hint">{hint}</p>}
      {children}
    </div>
  );
}

export function PageHead({ title, subtitle, children }) {
  return (
    <div className="page-head">
      <div>
        <h2>{title}</h2>
        {subtitle && <p>{subtitle}</p>}
      </div>
      <div className="page-actions">{children}</div>
    </div>
  );
}

export const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString([], { hour12: false }) : "—");

const ToastContext = createContext(() => {});
export const useToast = () => useContext(ToastContext);

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);
  const show = useCallback((message, tone = "ok") => setToast({ message, tone, id: Date.now() }), []);
  useEffect(() => {
    if (!toast) return undefined;
    const t = setTimeout(() => setToast(null), 3200);
    return () => clearTimeout(t);
  }, [toast]);
  return (
    <ToastContext.Provider value={show}>
      {children}
      {toast && <div className={`toast toast-${toast.tone}`} role="status">{toast.message}</div>}
    </ToastContext.Provider>
  );
}
