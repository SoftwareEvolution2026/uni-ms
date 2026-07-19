import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { Modal } from "../components/Modal";
import { useToast } from "../components/ToastProvider";
import type { Result } from "../types";

interface ResultForm {
  studentId: number;
  courseCode: string;
  term: string;
  grade: string;
  score: number;
  credits: number;
}

const emptyForm: ResultForm = {
  studentId: 0,
  courseCode: "",
  term: "",
  grade: "",
  score: 0,
  credits: 0,
};

export default function Results() {
  const toast = useToast();
  const [results, setResults] = useState<Result[]>([]);
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [pendingDelete, setPendingDelete] = useState<Result | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function load() {
    const { data } = await api.get<Result[]>("/results");
    setResults(data);
  }

  useEffect(() => {
    load();
  }, []);

  function openCreate() {
    setEditingId(null);
    setForm(emptyForm);
    setFormOpen(true);
  }

  function openEdit(result: Result) {
    setEditingId(result.id);
    setForm({
      studentId: result.studentId,
      courseCode: result.courseCode,
      term: result.term,
      grade: result.grade,
      score: result.score,
      credits: result.credits,
    });
    setFormOpen(true);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      if (editingId !== null) {
        await api.put(`/results/${editingId}`, form);
        toast.success("Result updated");
      } else {
        await api.post("/results", form);
        toast.success("Result created");
      }
      setFormOpen(false);
      await load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        "Failed to save result";
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
      await api.delete(`/results/${target.id}`);
      toast.success("Result deleted");
      await load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        "Failed to delete result";
      toast.error(message);
    }
  }

  const isEditing = editingId !== null;

  return (
    <div className="page">
      <header className="topbar">
        <strong>uni-ms · Results</strong>
        <div>
          <button onClick={openCreate}>+ New result</button>
          <Link className="ghost-link" to="/dashboard">
            ← Dashboard
          </Link>
        </div>
      </header>

      <main>
        <h2>Results ({results.length})</h2>
        <table className="table">
          <thead>
            <tr>
              <th>Student</th>
              <th>Course</th>
              <th>Term</th>
              <th>Grade</th>
              <th>Score</th>
              <th>Credits</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {results.map((result) => (
              <tr key={result.id}>
                <td>{result.studentId}</td>
                <td>{result.courseCode}</td>
                <td>{result.term}</td>
                <td>{result.grade}</td>
                <td>{result.score}</td>
                <td>{result.credits}</td>
                <td className="row-actions">
                  <button className="ghost" onClick={() => openEdit(result)}>
                    Edit
                  </button>
                  <button className="danger" onClick={() => setPendingDelete(result)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </main>

      <Modal open={formOpen} title={isEditing ? "Edit result" : "New result"} onClose={() => setFormOpen(false)}>
        <form onSubmit={onSubmit}>
          <label>
            Student ID
            <input
              value={form.studentId}
              onChange={(e) => setForm({ ...form, studentId: Number(e.target.value) })}
              type="number"
              min="1"
              required
            />
          </label>
          <label>
            Course code
            <input
              value={form.courseCode}
              onChange={(e) => setForm({ ...form, courseCode: e.target.value })}
              required
            />
          </label>
          <label>
            Term
            <input
              value={form.term}
              onChange={(e) => setForm({ ...form, term: e.target.value })}
              required
            />
          </label>
          <label>
            Grade
            <input
              value={form.grade}
              onChange={(e) => setForm({ ...form, grade: e.target.value })}
              required
            />
          </label>
          <label>
            Score
            <input
              value={form.score}
              onChange={(e) => setForm({ ...form, score: Number(e.target.value) })}
              type="number"
              min="0"
              step="0.1"
              required
            />
          </label>
          <label>
            Credits
            <input
              value={form.credits}
              onChange={(e) => setForm({ ...form, credits: Number(e.target.value) })}
              type="number"
              min="1"
              required
            />
          </label>
          <div className="modal-actions">
            <button type="button" className="ghost" onClick={() => setFormOpen(false)}>
              Cancel
            </button>
            <button disabled={submitting}>{submitting ? "Saving…" : isEditing ? "Save changes" : "Create result"}</button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete result"
        message="Are you sure you want to delete this result?"
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}
