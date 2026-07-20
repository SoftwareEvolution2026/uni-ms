import { FormEvent, useEffect, useState } from "react";
import { Modal } from "../../components/Modal";
import { departmentApi, errorMessage, problemFrom } from "../../api/catalogApi";
import type { Department, DepartmentInput, RegistryStatus } from "../../types";

interface Props {
  open: boolean;
  department: Department | null;
  onClose: () => void;
  onSaved: (department: Department) => void;
}

const initial: DepartmentInput = { departmentName: "", departmentCode: "", faculty: "", description: null, status: "ACTIVE" };

export function DepartmentForm({ open, department, onClose, onSaved }: Props) {
  const [form, setForm] = useState<DepartmentInput>(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setForm(department ? {
      departmentName: department.departmentName, departmentCode: department.departmentCode,
      faculty: department.faculty, description: department.description,
      status: department.status, version: department.version,
    } : initial);
    setErrors({});
  }, [department, open]);

  function change<K extends keyof DepartmentInput>(key: K, value: DepartmentInput[K]) {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: "" }));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const clean = {
      ...form,
      departmentName: form.departmentName.trim(),
      departmentCode: form.departmentCode.trim().toUpperCase(),
      faculty: form.faculty.trim(),
      description: form.description?.trim() || null,
    };
    const clientErrors: Record<string, string> = {};
    if (!clean.departmentName) clientErrors.departmentName = "Department name is required.";
    if (!clean.departmentCode) clientErrors.departmentCode = "Department code is required.";
    if (!clean.faculty) clientErrors.faculty = "Faculty is required.";
    if (Object.keys(clientErrors).length) { setErrors(clientErrors); return; }
    setSaving(true);
    try {
      const saved = department
        ? await departmentApi.update(department.id, clean)
        : await departmentApi.create(clean);
      onSaved(saved);
    } catch (reason) {
      setErrors({ ...problemFrom(reason)?.fieldErrors, form: errorMessage(reason, "Department could not be saved.") });
    } finally { setSaving(false); }
  }

  return <Modal open={open} title={department ? "Edit department" : "Create department"} onClose={onClose}>
    <form onSubmit={submit} className="registry-form">
      {errors.form && <div className="form-error">{errors.form}</div>}
      <div className="form-grid two">
        <label>Department name<input value={form.departmentName} maxLength={150} onChange={(e) => change("departmentName", e.target.value)} aria-invalid={!!errors.departmentName} />{errors.departmentName && <small className="field-error">{errors.departmentName}</small>}</label>
        <label>Department code<input value={form.departmentCode} maxLength={30} onChange={(e) => change("departmentCode", e.target.value.toUpperCase())} aria-invalid={!!errors.departmentCode} />{errors.departmentCode && <small className="field-error">{errors.departmentCode}</small>}</label>
      </div>
      <label>Faculty<input value={form.faculty} maxLength={150} onChange={(e) => change("faculty", e.target.value)} aria-invalid={!!errors.faculty} />{errors.faculty && <small className="field-error">{errors.faculty}</small>}</label>
      <label>Description<textarea rows={4} maxLength={1000} value={form.description ?? ""} onChange={(e) => change("description", e.target.value)} />{errors.description && <small className="field-error">{errors.description}</small>}</label>
      <label>Status<select value={form.status} onChange={(e) => change("status", e.target.value as RegistryStatus)}><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select></label>
      <div className="modal-actions"><button type="button" className="button secondary" onClick={onClose} disabled={saving}>Cancel</button><button className="button primary" disabled={saving}>{saving ? "Saving…" : department ? "Save changes" : "Create department"}</button></div>
    </form>
  </Modal>;
}
