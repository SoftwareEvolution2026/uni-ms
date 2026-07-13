import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function Dashboard() {
  const { user, logout } = useAuth();
  const roles = user?.roles.map((r) => r.replace("ROLE_", "")).join(", ");

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
          <button className="ghost" onClick={logout}>
            Logout
          </button>
        </div>
      </header>

      <main>
        <div className="welcome">
          <h1>Welcome, {user?.fullName}</h1>
          <p className="muted">
            Signed in as {user?.email}
          </p>
          <span className="badge">{roles}</span>
        </div>
      </main>
    </div>
  );
}
