import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import { Modal } from "../components/Modal";

export default function Dashboard() {
  const { user, logout } = useAuth();
  const toast = useToast();
  const roles = user?.roles.map((r) => r.replace("ROLE_", "")).join(", ");

  const [pwOpen, setPwOpen] = useState(false);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function changePassword(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.put("/auth/password", { currentPassword, newPassword });
      toast.success("Password changed");
      setPwOpen(false);
      setCurrentPassword("");
      setNewPassword("");
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        "Failed to change password";
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <header className="topbar">
        <strong>uni-ms</strong>
        <div>
          {user?.roles.includes("ROLE_ADMIN") && (
            <Link className="ghost-link" to="/users">
              Manage Users
            </Link>
          )}
          <button className="ghost" onClick={() => setPwOpen(true)}>
            Change password
          </button>
          <button className="ghost" onClick={logout}>
            Logout
          </button>
        </div>
      </header>

      <main>
        <div className="welcome">
          <h1>Welcome, {user?.fullName}</h1>
          <p className="muted">Signed in as {user?.email}</p>
          <span className="badge">{roles}</span>
        </div>
      </main>

      <Modal open={pwOpen} title="Change password" onClose={() => setPwOpen(false)}>
        <form onSubmit={changePassword}>
          <label>
            Current password
            <input
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              type="password"
              required
            />
          </label>
          <label>
            New password
            <input
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              type="password"
              minLength={8}
              required
            />
          </label>
          <div className="modal-actions">
            <button type="button" className="ghost" onClick={() => setPwOpen(false)}>
              Cancel
            </button>
            <button disabled={submitting}>{submitting ? "Saving…" : "Change password"}</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
