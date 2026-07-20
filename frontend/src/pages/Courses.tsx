import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { courseApi, departmentApi, errorMessage } from "../api/catalogApi";
import { useAuth } from "../auth/AuthContext";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { useToast } from "../components/ToastProvider";
import { CourseForm } from "../features/courses/CourseForm";
import type { Course, Department, PageResponse } from "../types";

const blankPage: PageResponse<Course> = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true, sort: [] };

export default function Courses() {
  const { user } = useAuth();
  const toast = useToast();
  const [urlParams] = useSearchParams();
  const [result, setResult] = useState(blankPage);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState(urlParams.get("status") ?? "");
  const [departmentId, setDepartmentId] = useState("");
  const [semester, setSemester] = useState("");
  const [academicYear, setAcademicYear] = useState("");
  const [sort, setSort] = useState("courseName,asc");
  const [page, setPage] = useState(0);
  const [trash, setTrash] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [selected, setSelected] = useState<Course | null>(null);
  const [viewing, setViewing] = useState<Course | null>(null);
  const [confirm, setConfirm] = useState<{ course: Course; permanent: boolean } | null>(null);
  const [acting, setActing] = useState(false);
  const departmentNames = useMemo(() => new Map(departments.map((department) => [department.id, department])), [departments]);

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try { setResult(await courseApi.list({ search, status, departmentId: departmentId ? Number(departmentId) : undefined, semester, academicYear, page, size: 10, sort, trash })); }
    catch (reason) { setError(errorMessage(reason, "Courses could not be loaded.")); }
    finally { setLoading(false); }
  }, [academicYear, departmentId, page, search, semester, sort, status, trash]);

  useEffect(() => { void departmentApi.list({ page: 0, size: 100, sort: "departmentName,asc", status: "ACTIVE" }).then((data) => setDepartments(data.content)).catch(() => setDepartments([])); }, []);
  useEffect(() => { const timer = window.setTimeout(() => void load(), 250); return () => window.clearTimeout(timer); }, [load]);

  function changeFilter(action: () => void) { setPage(0); action(); }
  function create() { setSelected(null); setFormOpen(true); }
  function edit(course: Course) { setSelected(course); setFormOpen(true); }

  async function remove() {
    if (!confirm) return;
    setActing(true);
    try {
      if (confirm.permanent) await courseApi.permanentlyRemove(confirm.course.id); else await courseApi.remove(confirm.course.id);
      toast.success(confirm.permanent ? "Course permanently deleted." : "Course moved to trash."); setConfirm(null); await load();
    } catch (reason) { toast.error(errorMessage(reason, "Course could not be deleted.")); }
    finally { setActing(false); }
  }

  async function restore(course: Course) {
    setActing(true);
    try { await courseApi.restore(course.id); toast.success("Course restored."); await load(); }
    catch (reason) { toast.error(errorMessage(reason, "Course could not be restored.")); }
    finally { setActing(false); }
  }

  return <>
    <header className="page-heading"><div><span className="eyebrow">Curriculum</span><h1>Courses</h1><p>Manage the course catalogue, credits and academic periods.</p></div><button className="button primary" onClick={create}>＋ New course</button></header>
    <section className="panel registry-panel">
      <div className="registry-tabs"><button className={!trash ? "active" : ""} onClick={() => changeFilter(() => setTrash(false))}>Current</button><button className={trash ? "active" : ""} onClick={() => changeFilter(() => setTrash(true))}>Trash</button></div>
      <div className="filters course-filters"><label className="search-box"><span>⌕</span><input placeholder="Search courses…" value={search} onChange={(e) => changeFilter(() => setSearch(e.target.value))} /></label><select value={departmentId} onChange={(e) => changeFilter(() => setDepartmentId(e.target.value))}><option value="">All departments</option>{departments.map((department) => <option key={department.id} value={department.id}>{department.departmentCode}</option>)}</select><select value={semester} onChange={(e) => changeFilter(() => setSemester(e.target.value))}><option value="">All semesters</option><option value="SEMESTER_1">Semester 1</option><option value="SEMESTER_2">Semester 2</option><option value="SUMMER">Summer</option></select><select value={status} onChange={(e) => changeFilter(() => setStatus(e.target.value))}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select><input className="year-filter" placeholder="2026/2027" value={academicYear} onChange={(e) => changeFilter(() => setAcademicYear(e.target.value))} /><select value={sort} onChange={(e) => changeFilter(() => setSort(e.target.value))}><option value="courseName,asc">Name A–Z</option><option value="courseCode,asc">Code A–Z</option><option value="createdAt,desc">Newest first</option><option value="creditUnits,desc">Most credits</option></select></div>
      {error ? <div className="state-card error-state"><span>!</span><h3>Unable to load courses</h3><p>{error}</p><button className="button secondary" onClick={load}>Try again</button></div> : loading ? <div className="table-loading"><span /><span /><span /></div> : !result.content.length ? <div className="state-card"><span>▤</span><h3>{trash ? "Trash is empty" : "No courses found"}</h3><p>{search || status || departmentId || semester ? "Try adjusting your filters." : "Create the first course in your academic catalogue."}</p>{!trash && <button className="button primary" onClick={create}>Create course</button>}</div> : <div className="table-wrap"><table className="data-table"><thead><tr><th>Course</th><th>Department</th><th>Semester</th><th>Credits</th><th>Status</th><th aria-label="Actions" /></tr></thead><tbody>{result.content.map((course) => <tr key={course.id}><td><div className="entity-cell"><span>{course.courseCode.slice(0, 2)}</span><div><strong>{course.courseName}</strong><small>{course.courseCode} · {course.academicYear}</small></div></div></td><td>{departmentNames.get(course.departmentId)?.departmentCode ?? `#${course.departmentId}`}</td><td>{semesterLabel(course.semester)}</td><td>{course.creditUnits}</td><td><span className={`status ${course.status.toLowerCase()}`}>{course.status.toLowerCase()}</span></td><td><div className="row-menu"><button onClick={() => setViewing(course)}>View</button>{trash ? <><button onClick={() => restore(course)} disabled={acting}>Restore</button>{user?.role === "ADMIN" && <button className="text-danger" onClick={() => setConfirm({ course, permanent: true })}>Delete forever</button>}</> : <><button onClick={() => edit(course)}>Edit</button><button className="text-danger" onClick={() => setConfirm({ course, permanent: false })}>Delete</button></>}</div></td></tr>)}</tbody></table></div>}
      <Pagination page={result.page} size={result.size} totalElements={result.totalElements} totalPages={result.totalPages} onPage={setPage} />
    </section>
    <CourseForm open={formOpen} course={selected} departments={departments} onClose={() => setFormOpen(false)} onSaved={() => { setFormOpen(false); toast.success(selected ? "Course updated." : "Course created."); void load(); }} />
    <Modal open={!!viewing} title="Course details" onClose={() => setViewing(null)}>{viewing && <div className="detail-list"><div><small>Code</small><strong>{viewing.courseCode}</strong></div><div><small>Name</small><strong>{viewing.courseName}</strong></div><div><small>Department</small><strong>{departmentNames.get(viewing.departmentId)?.departmentName ?? `#${viewing.departmentId}`}</strong></div><div><small>Status</small><strong>{viewing.status}</strong></div><div><small>Semester</small><strong>{semesterLabel(viewing.semester)}</strong></div><div><small>Academic year</small><strong>{viewing.academicYear}</strong></div><div><small>Credit units</small><strong>{viewing.creditUnits}</strong></div><div className="wide"><small>Description</small><p>{viewing.description || "No description provided."}</p></div></div>}</Modal>
    <ConfirmDialog open={!!confirm} title={confirm?.permanent ? "Permanently delete course?" : "Move course to trash?"} message={confirm?.permanent ? "This course will be permanently removed and cannot be recovered." : "The course can be restored from the trash later."} confirmLabel={confirm?.permanent ? "Delete forever" : "Move to trash"} busy={acting} onConfirm={remove} onCancel={() => setConfirm(null)} />
  </>;
}

function semesterLabel(value: string) { return value === "SEMESTER_1" ? "Semester 1" : value === "SEMESTER_2" ? "Semester 2" : "Summer"; }
