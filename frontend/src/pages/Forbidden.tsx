import { Link } from "react-router-dom";

export default function Forbidden() {
  return <div className="error-page"><span>403</span><h1>Access denied</h1><p>Your account does not have permission to view this page.</p><Link className="button primary" to="/dashboard">Back to dashboard</Link></div>;
}
