import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import Icon from "../../components/Icon";
import { clearAuthSession } from "../../utils/auth";
import "./AdminLayout.css";

const NAV = [
  { path: "/admin", label: "Overview", end: true, icon: "sparkles" },
  { path: "/admin/users", label: "Users", icon: "user" },
  { path: "/admin/workspaces", label: "Workspaces", icon: "building" },
  { path: "/admin/marketplace", label: "Marketplace", icon: "compass" },
  { path: "/admin/payments", label: "Payments", icon: "wallet" },
  { path: "/admin/subscription-plans", label: "Plans", icon: "library" },
  { path: "/admin/audit-logs", label: "Audit logs", icon: "shield" },
  { path: "/admin/ai-models", label: "AI models", icon: "settings" },
];

export default function AdminLayout({ title, subtitle, children }) {
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === "Escape") setMobileOpen(false);
    };
    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, []);

  const handleLogout = () => {
    clearAuthSession();
    navigate("/login");
  };

  return (
    <div className="admin-shell">
      <button
        aria-label="Close admin navigation"
        className={`admin-backdrop ${mobileOpen ? "is-visible" : ""}`}
        onClick={() => setMobileOpen(false)}
        type="button"
      />
      <aside className={`admin-sidebar ${mobileOpen ? "is-open" : ""}`}>
        <div className="admin-brand">
          <span className="admin-brand-icon"><Icon name="shield" size={20} /></span>
          <div><h2>AI Studio</h2><p>Admin console</p></div>
          <button aria-label="Close navigation" className="icon-button admin-close" onClick={() => setMobileOpen(false)} type="button">
            <Icon name="close" />
          </button>
        </div>

        <nav className="admin-nav">
          <p>Platform</p>
          {NAV.map((item) => (
            <NavLink
              className={({ isActive }) => `admin-nav-link ${isActive ? "active" : ""}`}
              end={item.end}
              key={item.path}
              onClick={() => setMobileOpen(false)}
              to={item.path}
            >
              <Icon name={item.icon} size={18} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="admin-sidebar-actions">
          <button className="btn btn-secondary" onClick={() => navigate("/dashboard")} type="button">
            <Icon name="arrowRight" size={16} className="admin-back-icon" /> User app
          </button>
          <button className="btn btn-secondary" onClick={handleLogout} type="button">
            <Icon name="logout" size={16} /> Log out
          </button>
        </div>
      </aside>

      <main className="admin-main">
        <header className="admin-mobile-bar">
          <button aria-label="Open navigation" className="icon-button" onClick={() => setMobileOpen(true)} type="button"><Icon name="menu" /></button>
          <strong>Admin console</strong>
          <span />
        </header>
        <div className="admin-content">
          <header className="admin-header">
            <p>Platform administration</p>
            <h1>{title}</h1>
            {subtitle && <span>{subtitle}</span>}
          </header>
          {children}
        </div>
      </main>
    </div>
  );
}
