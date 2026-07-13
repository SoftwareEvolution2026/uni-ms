import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import { Modal } from "../components/Modal";
import { ConfirmDialog } from "../components/ConfirmDialog";
import type { User } from "../types";

const ALL_ROLES = ["ROLE_ADMIN", "ROLE_LECTURER", "ROLE_STAFF", "ROLE_STUDENT"];

const emptyForm = {
  fullName: "",
  email: "",
  password: "",
  roles: ["ROLE_STUDENT"] as string[],
  enabled: true,
};

export default function Users() {
  const { user: currentUser } = useAuth();
  const toast = useToast();
  const [users, setUsers] = useState<User[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
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
    setEditingId(null);
    setForm(emptyForm);
    setFormOpen(true);
  }

  function openEdit(u: User) {
    setEditingId(u.id);
    setForm({ fullName: u.fullName, email: u.email, password: "", roles: u.roles, enabled: u.enabled });
    setFormOpen(true);
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
      if (editingId !== null) {
        await api.put(`/users/${editingId}`, {
          fullName: form.fullName,
          email: form.email,
          roles: form.roles,
          enabled: form.enabled,
        });
        toast.success(`User ${form.email} updated`);
      } else {
        await api.post("/users", {
          fullName: form.fullName,
          email: form.email,
          password: form.password,
          roles: form.roles,
        });
        toast.success(`User ${form.email} created`);
      }
      setFormOpen(false);
      await load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        "Failed to save user";
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

  const isEditing = editingId !== null;

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
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.fullName}</td>
                <td>{u.email}</td>
                <td>{u.roles.map((r) => r.replace("ROLE_", "")).join(", ")}</td>
                <td>{u.enabled ? "Active" : "Disabled"}</td>
                <td className="row-actions">
                  <button className="ghost" onClick={() => openEdit(u)}>
                    Edit
                  </button>
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

      <Modal
        open={formOpen}
        title={isEditing ? "Edit user" : "Create user"}
        onClose={() => setFormOpen(false)}
      >
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
          {!isEditing && (
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
          )}
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
          {isEditing && (
            <label className="checkbox">
              <input
                type="checkbox"
                checked={form.enabled}
                onChange={(e) => setForm({ ...form, enabled: e.target.checked })}
              />
              Account active
            </label>
          )}
          <div className="modal-actions">
            <button type="button" className="ghost" onClick={() => setFormOpen(false)}>
              Cancel
            </button>
            <button disabled={submitting || form.roles.length === 0}>
              {submitting ? "Saving…" : isEditing ? "Save changes" : "Create user"}
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
