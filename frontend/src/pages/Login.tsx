import { FormEvent, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";

export default function Login() {
  const { login, user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await login(email, password);
      toast.success("Welcome back!");
      navigate("/dashboard");
    } catch (error: unknown) {
      const detail = (error as { response?: { data?: { detail?: string } } })
        ?.response?.data?.detail;
      toast.error(detail ?? "Unable to sign in. Check your details and try again.");
    } finally {
      setSubmitting(false);
    }
  }

  if (user) return <Navigate to="/dashboard" replace />;

  return (
    <div className="login-page">
      <section className="login-hero">
        <div className="brand light"><span className="brand-mark">U</span><div><strong>UniRegistry</strong><small>Academic management</small></div></div>
        <div><span className="eyebrow">Built for academic teams</span><h1>One registry.<br />Complete clarity.</h1><p>Manage departments, courses and academic operations from a secure, unified workspace.</p></div>
        <small>University Academic Registry</small>
      </section>
      <section className="login-panel">
        <form className="login-card" onSubmit={onSubmit}>
          <div><span className="eyebrow">Welcome back</span><h2>Sign in to your account</h2><p>Enter your institutional credentials to continue.</p></div>
          <label>Email address<input value={email} onChange={(e) => setEmail(e.target.value)} type="email" autoComplete="email" placeholder="name@university.edu" required /></label>
          <label>Password
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            required autoComplete="current-password" placeholder="Enter your password"
          />
          </label>
          <button className="button primary full" disabled={submitting}>{submitting ? "Signing in…" : "Sign in"}</button>
          <p className="login-help">Need access? Contact your system administrator.</p>
        </form>
      </section>
    </div>
  );
}
