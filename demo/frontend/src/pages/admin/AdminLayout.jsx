import { useNavigate, useLocation } from "react-router-dom";
import { clearAuthSession } from "../../utils/auth";
import "./AdminLayout.css";

const NAV = [
  { path: "/admin", label: "Overview", end: true, gradient: "var(--primary-gradient)" },
  { path: "/admin/users", label: "Users", gradient: "var(--secondary-gradient)" },
  { path: "/admin/workspaces", label: "Workspaces", gradient: "var(--accent-gradient)" },
  { path: "/admin/marketplace", label: "Marketplace", gradient: "var(--success-gradient)" },
  { path: "/admin/payments", label: "Payments", gradient: "var(--warning-gradient)" },
  { path: "/admin/subscription-plans", label: "Subscription Plans", gradient: "linear-gradient(135deg, #8b5cf6, #ec4899)" },
  { path: "/admin/audit-logs", label: "Audit logs", gradient: "var(--danger-gradient)" },
  { path: "/admin/ai-models", label: "AI models", gradient: "var(--dark-gradient)" },
];

export default function AdminLayout({ title, subtitle, children }) {
  const navigate = useNavigate();
  const location = useLocation();

  const isActive = (path, end) => {
    if (end) return location.pathname === path;
    return location.pathname.startsWith(path);
  };

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-brand">
          <span className="admin-brand-icon gradient-bg animate-gradient">⚙</span>
          <div>
            <h2 className="gradient-text">Admin Console</h2>
            <p>Platform management</p>
          </div>
        </div>

        <nav className="admin-nav">
          {NAV.map((item) => (
            <button
              key={item.path}
              type="button"
              className={`admin-nav-link ${isActive(item.path, item.end) ? "active" : ""}`}
              onClick={() => navigate(item.path)}
              style={isActive(item.path, item.end) ? { background: item.gradient } : {}}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="admin-sidebar-actions">
          <button type="button" className="admin-link-btn btn btn-secondary" onClick={() => navigate("/dashboard")}>
            ← User app
          </button>
          <button
            type="button"
            className="admin-logout btn btn-danger"
            onClick={() => {
              clearAuthSession();
              navigate("/login");
            }}
          >
            Log out
          </button>
        </div>
      </aside>

      <main className="admin-main">
        <header className="admin-header">
          <h1 className="gradient-text">{title}</h1>
          {subtitle && <p>{subtitle}</p>}
        </header>
        {children}
      </main>
    </div>
  );
}
