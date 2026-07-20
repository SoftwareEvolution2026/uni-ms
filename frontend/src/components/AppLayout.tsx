import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "./ToastProvider";

const navigation = [
  { to: "/dashboard", label: "Dashboard", icon: "▦" },
  { to: "/departments", label: "Departments", icon: "⌂" },
  { to: "/courses", label: "Courses", icon: "▤" },
];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const title = navigation.find((item) => location.pathname.startsWith(item.to))?.label
    ?? "Academic Registry";

  async function signOut() {
    await logout();
    toast.info("You have been signed out");
    navigate("/login", { replace: true });
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${open ? "sidebar-open" : ""}`}>
        <div className="brand"><span className="brand-mark">U</span><div><strong>UniRegistry</strong><small>Academic management</small></div></div>
        <nav aria-label="Main navigation">
          {navigation.map((item) => (
            <NavLink key={item.to} to={item.to} onClick={() => setOpen(false)}>
              <span className="nav-icon">{item.icon}</span>{item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-profile">
          <div className="avatar">{user?.name.charAt(0).toUpperCase()}</div>
          <div><strong>{user?.name}</strong><small>{user?.role.replace("_", " ")}</small></div>
        </div>
      </aside>
      {open && <button className="sidebar-scrim" aria-label="Close menu" onClick={() => setOpen(false)} />}
      <section className="app-main">
        <header className="app-topbar">
          <div><button className="menu-button" onClick={() => setOpen(true)} aria-label="Open menu">☰</button><div><small>Academic Registry /</small><strong>{title}</strong></div></div>
          <div className="topbar-actions"><span>{user?.email}</span><button className="button secondary compact" onClick={signOut}>Sign out</button></div>
        </header>
        <main className="content"><Outlet /></main>
      </section>
    </div>
  );
}
