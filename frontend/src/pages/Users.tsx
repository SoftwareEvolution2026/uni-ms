import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import { Modal } from "../components/Modal";
import { ConfirmDialog } from "../components/ConfirmDialog";
import type { User } from "../types";

const ALL_ROLES = ["ROLE_ADMIN", "ROLE_LECTURER", "ROLE_STAFF", "ROLE_STUDENT"];

const emptyForm = { fullName: "", email: "", password: "", roles: ["ROLE_STUDENT"] };

export default function Users() {
  const { user: currentUser } = useAuth();
  const toast = useToast();
  const [users, setUsers] = useState<User[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [createOpen, setCreateOpen] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<User | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function load() {
    const { data } = await api.get<User[]>("/users");
    setUsers(data);
  }

  useEffect(() => {
    load();
  }, []);

  function openCreate() {
    setForm(emptyForm);
    setCreateOpen(true);
  }

  function toggleRole(role: string) {
    setForm((prev) => ({
      ...prev,
      roles: prev.roles.includes(role)
        ? prev.roles.filter((r) => r !== role)
        : [...prev.roles, role],
    }));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.post("/users", form);
      setCreateOpen(false);
      toast.success(`User ${form.email} created`);
      await load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        "Failed to create user";
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) return;
    const target = pendingDelete;
    setPendingDelete(null);
    try {
      await api.delete(`/users/${target.id}`);
      toast.success(`Deleted ${target.email}`);
      await load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        "Failed to delete user";
      toast.error(message);
    }
  }

  return (
    <div className="page">
      <header className="topbar">
        <strong>uni-ms · User Management</strong>
        <div>
          <button onClick={openCreate}>+ New user</button>
          <Link className="ghost-link" to="/dashboard">
            ← Dashboard
          </Link>
        </div>
      </header>

      <main>
        <h2>All users ({users.length})</h2>
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Roles</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.fullName}</td>
                <td>{u.email}</td>
                <td>{u.roles.map((r) => r.replace("ROLE_", "")).join(", ")}</td>
                <td>
                  {u.email !== currentUser?.email && (
                    <button className="danger" onClick={() => setPendingDelete(u)}>
                      Delete
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </main>

      <Modal open={createOpen} title="Create user" onClose={() => setCreateOpen(false)}>
        <form onSubmit={onSubmit}>
          <label>
            Full name
            <input
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              required
            />
          </label>
          <label>
            Email
            <input
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              type="email"
              required
            />
          </label>
          <label>
            Temporary password
            <input
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              type="text"
              minLength={8}
              required
            />
          </label>
          <fieldset className="roles">
            <legend>Roles</legend>
            {ALL_ROLES.map((role) => (
              <label key={role} className="checkbox">
                <input
                  type="checkbox"
                  checked={form.roles.includes(role)}
                  onChange={() => toggleRole(role)}
                />
                {role.replace("ROLE_", "")}
              </label>
            ))}
          </fieldset>
          <div className="modal-actions">
            <button type="button" className="ghost" onClick={() => setCreateOpen(false)}>
              Cancel
            </button>
            <button disabled={submitting || form.roles.length === 0}>
              {submitting ? "Creating…" : "Create user"}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete user"
        message={`Delete ${pendingDelete?.email}? This cannot be undone.`}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}
