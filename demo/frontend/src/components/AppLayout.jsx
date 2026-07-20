import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useWorkspace } from "../context/workspace-context";
import { useTheme } from "../context/theme-context";
import { clearAuthSession, isAdmin } from "../utils/auth";
import Icon from "./Icon";
import "./AppLayout.css";

const NAV = [
  { path: "/dashboard", label: "Create", icon: "sparkles" },
  { path: "/prompts", label: "Prompt library", icon: "library" },
  { path: "/marketplace", label: "Marketplace", icon: "compass" },
  { path: "/assets", label: "Assets", icon: "folder" },
  { path: "/workspaces", label: "Workspaces", icon: "building" },
  { path: "/wallet", label: "Billing", icon: "wallet" },
  { path: "/notifications", label: "Notifications", icon: "bell" },
  { path: "/settings", label: "Settings", icon: "settings" },
];

function Brand({ compact = false }) {
  return (
    <div className={`app-brand ${compact ? "app-brand--compact" : ""}`}>
      <span className="app-brand-mark" aria-hidden="true">
        <Icon name="sparkles" size={compact ? 18 : 21} />
      </span>
      {!compact && (
        <span className="app-brand-copy">
          <strong>AI Studio</strong>
          <small>Creative workspace</small>
        </span>
      )}
    </div>
  );
}

export default function AppLayout({ title, subtitle, children, actions }) {
  const navigate = useNavigate();
  const { credits, unreadCount, loading, workspaces, workspaceId, switchWorkspace } =
    useWorkspace();
  const { theme, toggleTheme } = useTheme();
  const [mobileOpen, setMobileOpen] = useState(false);

  const userName = localStorage.getItem("user_name") || "Studio member";
  const userEmail = localStorage.getItem("user_email") || "";
  const initials = userName
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase() || "AI";

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

  const closeNavigation = () => setMobileOpen(false);

  return (
    <div className="app-shell">
      <button
        aria-label="Close navigation"
        className={`app-sidebar-backdrop ${mobileOpen ? "is-visible" : ""}`}
        onClick={closeNavigation}
        tabIndex={mobileOpen ? 0 : -1}
        type="button"
      />

      <aside
        id="app-sidebar"
        aria-label="Primary navigation"
        className={`app-sidebar ${mobileOpen ? "is-open" : ""}`}
      >
        <div className="app-sidebar-top">
          <Brand />
          <button
            aria-label="Close navigation"
            className="icon-button sidebar-close"
            onClick={closeNavigation}
            type="button"
          >
            <Icon name="close" />
          </button>
        </div>

        <div className="workspace-switcher">
          <span className="workspace-switcher-icon">
            <Icon name="building" size={18} />
          </span>
          <label>
            <span>Workspace</span>
            <select
              aria-label="Active workspace"
              disabled={loading || workspaces.length === 0}
              onChange={(event) => switchWorkspace(event.target.value)}
              value={workspaceId || ""}
            >
              {workspaces.length === 0 ? (
                <option value="">{loading ? "Loading…" : "No workspace"}</option>
              ) : (
                workspaces.map((workspace) => (
                  <option key={workspace.id} value={workspace.id}>
                    {workspace.name}
                  </option>
                ))
              )}
            </select>
          </label>
          <Icon name="chevronDown" size={16} />
        </div>

        <nav className="app-sidebar-nav">
          <p className="nav-section-label">Workspace</p>
          {NAV.map((item) => (
            <NavLink
              className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
              key={item.path}
              onClick={closeNavigation}
              to={item.path}
            >
              <Icon className="nav-icon" name={item.icon} size={19} />
              <span className="nav-label">{item.label}</span>
              {item.path === "/notifications" && unreadCount > 0 && (
                <span className="nav-badge">{unreadCount > 99 ? "99+" : unreadCount}</span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-usage-card">
          <div className="usage-card-heading">
            <span><Icon name="bolt" size={16} /> Credits</span>
            <strong>{loading ? "—" : Number(credits || 0).toLocaleString()}</strong>
          </div>
          <div className="usage-track" aria-hidden="true">
            <span style={{ width: `${Math.min(100, Math.max(8, Number(credits || 0) / 10))}%` }} />
          </div>
          <NavLink onClick={closeNavigation} to="/wallet">
            Manage billing <Icon name="arrowRight" size={14} />
          </NavLink>
        </div>

        <div className="app-sidebar-footer">
          <div className="sidebar-profile">
            <span className="profile-avatar">{initials}</span>
            <span className="profile-copy">
              <strong>{userName}</strong>
              <small>{userEmail || "Signed in"}</small>
            </span>
          </div>
          <div className="profile-actions">
            {isAdmin() && (
              <button
                aria-label="Open admin console"
                className="icon-button"
                onClick={() => navigate("/admin")}
                title="Admin console"
                type="button"
              >
                <Icon name="shield" size={18} />
              </button>
            )}
            <button
              aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} mode`}
              className="icon-button"
              onClick={toggleTheme}
              title={`Switch to ${theme === "dark" ? "light" : "dark"} mode`}
              type="button"
            >
              <Icon name={theme === "dark" ? "sun" : "moon"} size={18} />
            </button>
            <button
              aria-label="Log out"
              className="icon-button"
              onClick={handleLogout}
              title="Log out"
              type="button"
            >
              <Icon name="logout" size={18} />
            </button>
          </div>
        </div>
      </aside>

      <main className="app-main">
        <header className="app-mobile-bar">
          <button
            aria-controls="app-sidebar"
            aria-expanded={mobileOpen}
            aria-label="Open navigation"
            className="icon-button"
            onClick={() => setMobileOpen(true)}
            type="button"
          >
            <Icon name="menu" />
          </button>
          <Brand compact />
          <NavLink aria-label="Notifications" className="icon-button mobile-notifications" to="/notifications">
            <Icon name="bell" size={19} />
            {unreadCount > 0 && <span />}
          </NavLink>
        </header>

        <div className="app-content">
          <header className="app-page-header">
            <div>
              <p className="page-eyebrow">AI Studio</p>
              <h1>{title}</h1>
              {subtitle && <p className="page-subtitle">{subtitle}</p>}
            </div>
            {actions && <div className="app-page-actions">{actions}</div>}
          </header>
          <div className="app-page-body">{children}</div>
        </div>
      </main>
    </div>
  );
}
