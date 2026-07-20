import { FormEvent, useEffect, useState } from "react";
import { courseApi, errorMessage, problemFrom } from "../../api/catalogApi";
import { Modal } from "../../components/Modal";
import type { Course, CourseInput, Department, RegistryStatus, Semester } from "../../types";

interface Props {
  open: boolean;
  course: Course | null;
  departments: Department[];
  onClose: () => void;
  onSaved: (course: Course) => void;
}

const initial: CourseInput = { departmentId: 0, courseName: "", courseCode: "", creditUnits: 3, semester: "SEMESTER_1", academicYear: "", description: null, status: "ACTIVE" };

export function CourseForm({ open, course, departments, onClose, onSaved }: Props) {
  const [form, setForm] = useState<CourseInput>(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setForm(course ? {
      departmentId: course.departmentId, courseName: course.courseName,
      courseCode: course.courseCode, creditUnits: course.creditUnits,
      semester: course.semester, academicYear: course.academicYear,
      description: course.description, status: course.status, version: course.version,
    } : { ...initial, departmentId: departments[0]?.id ?? 0 });
    setErrors({});
  }, [course, departments, open]);

  function change<K extends keyof CourseInput>(key: K, value: CourseInput[K]) {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: "" }));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const clean = { ...form, courseName: form.courseName.trim(), courseCode: form.courseCode.trim().toUpperCase(), academicYear: form.academicYear.trim(), description: form.description?.trim() || null };
    const client: Record<string, string> = {};
    if (!clean.departmentId) client.departmentId = "Select a department.";
    if (!clean.courseName) client.courseName = "Course name is required.";
    if (!clean.courseCode) client.courseCode = "Course code is required.";
    if (!/^\d{4}\/\d{4}$/.test(clean.academicYear)) client.academicYear = "Use the YYYY/YYYY format.";
    else { const [first, second] = clean.academicYear.split("/").map(Number); if (second !== first + 1) client.academicYear = "Academic years must be consecutive."; }
    if (clean.creditUnits < 1 || clean.creditUnits > 30) client.creditUnits = "Credit units must be between 1 and 30.";
    if (Object.keys(client).length) { setErrors(client); return; }
    setSaving(true);
    try {
      const saved = course ? await courseApi.update(course.id, clean) : await courseApi.create(clean);
      onSaved(saved);
    } catch (reason) { setErrors({ ...problemFrom(reason)?.fieldErrors, form: errorMessage(reason, "Course could not be saved.") }); }
    finally { setSaving(false); }
  }

  return <Modal open={open} title={course ? "Edit course" : "Create course"} onClose={onClose}>
    <form onSubmit={submit} className="registry-form">
      {errors.form && <div className="form-error">{errors.form}</div>}
      {!departments.length && <div className="form-error">Create an active department before adding a course.</div>}
      <label>Department<select value={form.departmentId} onChange={(e) => change("departmentId", Number(e.target.value))}><option value={0}>Select department</option>{departments.map((department) => <option key={department.id} value={department.id}>{department.departmentCode} — {department.departmentName}</option>)}</select>{errors.departmentId && <small className="field-error">{errors.departmentId}</small>}</label>
      <div className="form-grid two"><label>Course name<input value={form.courseName} maxLength={150} onChange={(e) => change("courseName", e.target.value)} />{errors.courseName && <small className="field-error">{errors.courseName}</small>}</label><label>Course code<input value={form.courseCode} maxLength={30} onChange={(e) => change("courseCode", e.target.value.toUpperCase())} />{errors.courseCode && <small className="field-error">{errors.courseCode}</small>}</label></div>
      <div className="form-grid three"><label>Credit units<input type="number" min={1} max={30} value={form.creditUnits} onChange={(e) => change("creditUnits", Number(e.target.value))} />{errors.creditUnits && <small className="field-error">{errors.creditUnits}</small>}</label><label>Semester<select value={form.semester} onChange={(e) => change("semester", e.target.value as Semester)}><option value="SEMESTER_1">Semester 1</option><option value="SEMESTER_2">Semester 2</option><option value="SUMMER">Summer</option></select></label><label>Academic year<input value={form.academicYear} placeholder="2026/2027" maxLength={9} onChange={(e) => change("academicYear", e.target.value)} />{errors.academicYear && <small className="field-error">{errors.academicYear}</small>}</label></div>
      <label>Description<textarea rows={3} maxLength={1000} value={form.description ?? ""} onChange={(e) => change("description", e.target.value)} /></label>
      <label>Status<select value={form.status} onChange={(e) => change("status", e.target.value as RegistryStatus)}><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select></label>
      <div className="modal-actions"><button type="button" className="button secondary" onClick={onClose} disabled={saving}>Cancel</button><button className="button primary" disabled={saving || !departments.length}>{saving ? "Saving…" : course ? "Save changes" : "Create course"}</button></div>
    </form>
  </Modal>;
}
