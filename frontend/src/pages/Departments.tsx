import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { departmentApi, errorMessage } from "../api/catalogApi";
import { useAuth } from "../auth/AuthContext";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { useToast } from "../components/ToastProvider";
import { DepartmentForm } from "../features/departments/DepartmentForm";
import type { Department, PageResponse } from "../types";

const blankPage: PageResponse<Department> = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true, sort: [] };

export default function Departments() {
  const { user } = useAuth();
  const toast = useToast();
  const [urlParams] = useSearchParams();
  const [result, setResult] = useState(blankPage);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState(urlParams.get("status") ?? "");
  const [faculty, setFaculty] = useState("");
  const [sort, setSort] = useState("departmentName,asc");
  const [page, setPage] = useState(0);
  const [trash, setTrash] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [selected, setSelected] = useState<Department | null>(null);
  const [viewing, setViewing] = useState<Department | null>(null);
  const [confirm, setConfirm] = useState<{ department: Department; permanent: boolean } | null>(null);
  const [acting, setActing] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try { setResult(await departmentApi.list({ search, status, faculty, page, size: 10, sort, trash })); }
    catch (reason) { setError(errorMessage(reason, "Departments could not be loaded.")); }
    finally { setLoading(false); }
  }, [faculty, page, search, sort, status, trash]);

  useEffect(() => { const timer = window.setTimeout(() => void load(), 250); return () => window.clearTimeout(timer); }, [load]);

  function edit(department: Department) { setSelected(department); setFormOpen(true); }
  function create() { setSelected(null); setFormOpen(true); }
  function changeFilter(action: () => void) { setPage(0); action(); }

  async function remove() {
    if (!confirm) return;
    setActing(true);
    try {
      if (confirm.permanent) await departmentApi.permanentlyRemove(confirm.department.id);
      else await departmentApi.remove(confirm.department.id);
      toast.success(confirm.permanent ? "Department permanently deleted." : "Department moved to trash.");
      setConfirm(null); await load();
    } catch (reason) { toast.error(errorMessage(reason, "Department could not be deleted.")); }
    finally { setActing(false); }
  }

  async function restore(department: Department) {
    setActing(true);
    try { await departmentApi.restore(department.id); toast.success("Department restored."); await load(); }
    catch (reason) { toast.error(errorMessage(reason, "Department could not be restored.")); }
    finally { setActing(false); }
  }

  return <>
    <header className="page-heading"><div><span className="eyebrow">Academic structure</span><h1>Departments</h1><p>Manage faculties, department status and course ownership.</p></div><button className="button primary" onClick={create}>＋ New department</button></header>
    <section className="panel registry-panel">
      <div className="registry-tabs"><button className={!trash ? "active" : ""} onClick={() => changeFilter(() => setTrash(false))}>Current</button><button className={trash ? "active" : ""} onClick={() => changeFilter(() => setTrash(true))}>Trash</button></div>
      <div className="filters"><label className="search-box"><span>⌕</span><input placeholder="Search name or code…" value={search} onChange={(e) => changeFilter(() => setSearch(e.target.value))} /></label><select value={status} onChange={(e) => changeFilter(() => setStatus(e.target.value))}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select><input placeholder="Faculty" value={faculty} onChange={(e) => changeFilter(() => setFaculty(e.target.value))} /><select value={sort} onChange={(e) => changeFilter(() => setSort(e.target.value))}><option value="departmentName,asc">Name A–Z</option><option value="departmentName,desc">Name Z–A</option><option value="departmentCode,asc">Code A–Z</option><option value="createdAt,desc">Newest first</option></select></div>
      {error ? <div className="state-card error-state"><span>!</span><h3>Unable to load departments</h3><p>{error}</p><button className="button secondary" onClick={load}>Try again</button></div> : loading ? <div className="table-loading"><span /><span /><span /></div> : !result.content.length ? <div className="state-card"><span>⌂</span><h3>{trash ? "Trash is empty" : "No departments found"}</h3><p>{search || status || faculty ? "Try adjusting your search or filters." : "Create the first department to structure your registry."}</p>{!trash && <button className="button primary" onClick={create}>Create department</button>}</div> : <div className="table-wrap"><table className="data-table"><thead><tr><th>Department</th><th>Faculty</th><th>Status</th><th>Courses</th><th>Updated</th><th aria-label="Actions" /></tr></thead><tbody>{result.content.map((department) => <tr key={department.id}><td><div className="entity-cell"><span>{department.departmentCode.slice(0, 2)}</span><div><strong>{department.departmentName}</strong><small>{department.departmentCode}</small></div></div></td><td>{department.faculty}</td><td><span className={`status ${department.status.toLowerCase()}`}>{department.status.toLowerCase()}</span></td><td>{department.courseCount}</td><td>{formatDate(department.updatedAt)}</td><td><div className="row-menu"><button onClick={() => setViewing(department)} title="View">View</button>{trash ? <><button onClick={() => restore(department)} disabled={acting}>Restore</button>{user?.role === "ADMIN" && <button className="text-danger" onClick={() => setConfirm({ department, permanent: true })}>Delete forever</button>}</> : <><button onClick={() => edit(department)}>Edit</button><button className="text-danger" onClick={() => setConfirm({ department, permanent: false })}>Delete</button></>}</div></td></tr>)}</tbody></table></div>}
      <Pagination page={result.page} size={result.size} totalElements={result.totalElements} totalPages={result.totalPages} onPage={setPage} />
    </section>
    <DepartmentForm open={formOpen} department={selected} onClose={() => setFormOpen(false)} onSaved={() => { setFormOpen(false); toast.success(selected ? "Department updated." : "Department created."); void load(); }} />
    <Modal open={!!viewing} title="Department details" onClose={() => setViewing(null)}>{viewing && <div className="detail-list"><div><small>Code</small><strong>{viewing.departmentCode}</strong></div><div><small>Name</small><strong>{viewing.departmentName}</strong></div><div><small>Faculty</small><strong>{viewing.faculty}</strong></div><div><small>Status</small><strong>{viewing.status}</strong></div><div className="wide"><small>Description</small><p>{viewing.description || "No description provided."}</p></div><div><small>Created</small><strong>{formatDate(viewing.createdAt)}</strong></div><div><small>Courses</small><strong>{viewing.courseCount}</strong></div></div>}</Modal>
    <ConfirmDialog open={!!confirm} title={confirm?.permanent ? "Permanently delete department?" : "Move department to trash?"} message={confirm?.permanent ? "This action cannot be undone. Permanent deletion is blocked while courses reference this department." : "The department will leave active lists and can be restored later."} confirmLabel={confirm?.permanent ? "Delete forever" : "Move to trash"} busy={acting} onConfirm={remove} onCancel={() => setConfirm(null)} />
  </>;
}

function formatDate(value: string) { return new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(new Date(value)); }
