import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { dashboardApi, errorMessage } from "../api/catalogApi";
import { useAuth } from "../auth/AuthContext";
import type { DashboardStatistics } from "../types";

const empty: DashboardStatistics = {
  totalDepartments: 0, activeDepartments: 0, inactiveDepartments: 0,
  totalCourses: 0, activeCourses: 0, inactiveCourses: 0,
};

export default function Dashboard() {
  const { user } = useAuth();
  const [statistics, setStatistics] = useState(empty);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function load() {
    setLoading(true);
    setError("");
    try { setStatistics(await dashboardApi.get()); }
    catch (reason) { setError(errorMessage(reason, "Dashboard statistics could not be loaded.")); }
    finally { setLoading(false); }
  }

  useEffect(() => { void load(); }, []);

  const cards = [
    ["Departments", statistics.totalDepartments, "All registered departments", "departments", "blue"],
    ["Active departments", statistics.activeDepartments, "Currently available", "departments?status=ACTIVE", "green"],
    ["Inactive departments", statistics.inactiveDepartments, "Temporarily inactive", "departments?status=INACTIVE", "amber"],
    ["Courses", statistics.totalCourses, "All registered courses", "courses", "violet"],
    ["Active courses", statistics.activeCourses, "Currently delivered", "courses?status=ACTIVE", "teal"],
    ["Inactive courses", statistics.inactiveCourses, "Not currently delivered", "courses?status=INACTIVE", "rose"],
  ] as const;

  return (
    <>
      <header className="page-heading"><div><span className="eyebrow">Overview</span><h1>Good day, {user?.name.split(" ")[0]}</h1><p>Here is the current state of your academic registry.</p></div><button className="button secondary" onClick={load}>↻ Refresh</button></header>
      {error && <div className="error-banner"><span>{error}</span><button onClick={load}>Retry</button></div>}
      <section className="stat-grid" aria-busy={loading}>
        {cards.map(([label, value, note, path, tone]) => (
          <Link to={`/${path}`} className={`stat-card tone-${tone}`} key={label}>
            <div className="stat-icon">{label.includes("Course") || label.includes("course") ? "▤" : "⌂"}</div>
            <span>{label}</span><strong>{loading ? "—" : value}</strong><small>{note}</small>
          </Link>
        ))}
      </section>
      <section className="panel quick-panel"><div><span className="eyebrow">Quick access</span><h2>Registry management</h2></div><div className="quick-links"><Link to="/departments"><span>⌂</span><div><strong>Manage departments</strong><small>Structure, status and faculty</small></div><b>→</b></Link><Link to="/courses"><span>▤</span><div><strong>Manage courses</strong><small>Curriculum and academic periods</small></div><b>→</b></Link></div></section>
    </>
  );
}
