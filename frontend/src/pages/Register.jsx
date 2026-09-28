import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth";
import { ErrorBanner } from "../ui";

export default function Register() {
  const { register } = useAuth();
  const [form, setForm] = useState({ organizationName: "", username: "", password: "", confirm: "" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirm) {
      setError({ message: "Passwords do not match" });
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await register(form.organizationName.trim(), form.username.trim(), form.password);
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={submit}>
        <h1>Create your organization</h1>
        <p className="muted">You will be the tenant admin for this organization.</p>
        <ErrorBanner error={error} />
        <label>Organization name<input autoFocus value={form.organizationName} onChange={set("organizationName")} required /></label>
        <label>Username<input autoComplete="username" value={form.username} onChange={set("username")} required /></label>
        <label>Password<input type="password" autoComplete="new-password" value={form.password} onChange={set("password")} required />
          <span className="hint">At least 8 characters, with a letter and a digit.</span>
        </label>
        <label>Confirm password<input type="password" autoComplete="new-password" value={form.confirm} onChange={set("confirm")} required /></label>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? "Creating…" : "Create account"}</button>
        <p className="auth-alt">Already registered? <Link to="/login">Sign in</Link></p>
      </form>
    </div>
  );
}
