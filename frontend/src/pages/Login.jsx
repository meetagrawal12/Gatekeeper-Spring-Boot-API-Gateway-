import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth";
import { ErrorBanner } from "../ui";

export default function Login() {
  const { login } = useAuth();
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(form.username.trim(), form.password);
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={submit}>
        <h1>Sign in</h1>
        <p className="muted">Manage your gateway routes, policies and access.</p>
        <ErrorBanner error={error} />
        <label>Username
          <input autoFocus autoComplete="username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
        </label>
        <label>Password
          <input type="password" autoComplete="current-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
        </label>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? "Signing in…" : "Sign in"}</button>
        <p className="auth-alt">New organization? <Link to="/register">Create an account</Link></p>
      </form>
    </div>
  );
}
