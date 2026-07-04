import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useWorkspace } from "../context/WorkspaceContext";
import { useTheme } from "../context/ThemeContext";
import { isAdmin, clearAuthSession } from "../utils/auth";
import "./AppLayout.css";

const NAV = [
  { path: "/dashboard", label: "Prompt Studio", icon: "✨", gradient: "var(--primary-gradient)", emoji: "🎨" },
  { path: "/prompts", label: "Prompt Library", icon: "📝", gradient: "var(--secondary-gradient)", emoji: "📚" },
  { path: "/marketplace", label: "Marketplace", icon: "🌍", gradient: "var(--accent-gradient)", emoji: "🛍️" },
  { path: "/assets", label: "Cloud Assets", icon: "📁", gradient: "var(--success-gradient)", emoji: "☁️" },
  { path: "/workspaces", label: "Workspaces", icon: "🏢", gradient: "var(--info-gradient)", emoji: "🏢" },
  { path: "/wallet", label: "Billing & Wallet", icon: "💳", gradient: "var(--warning-gradient)", emoji: "💰" },
  { path: "/notifications", label: "Notifications", icon: "🔔", gradient: "var(--danger-gradient)", emoji: "💬" },
  { path: "/settings", label: "Settings", icon: "⚙️", gradient: "var(--dark-gradient)", emoji: "🔧" },
];

const AVATAR_EMOJIS = ["🚀", "🎯", "💡", "🌟", "🎪", "🎭", "🎨", "🎬", "🎮", "🎲"];

export default function AppLayout({ title, subtitle, children, actions }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { credits, unreadCount, loading, workspaces, workspaceId, switchWorkspace } =
    useWorkspace();
  const { theme, toggleTheme } = useTheme();
  const [avatarEmoji, setAvatarEmoji] = useState("🚀");
  const [floatingEmojis, setFloatingEmojis] = useState([]);
  const [sidebarVisible, setSidebarVisible] = useState(() => {
    const saved = localStorage.getItem('sidebarVisible');
    return saved !== null ? JSON.parse(saved) : true;
  });

  useEffect(() => {
    setAvatarEmoji(AVATAR_EMOJIS[Math.floor(Math.random() * AVATAR_EMOJIS.length)]);
  }, []);

  useEffect(() => {
    localStorage.setItem('sidebarVisible', JSON.stringify(sidebarVisible));
  }, [sidebarVisible]);

  const toggleSidebar = () => {
    setSidebarVisible(!sidebarVisible);
  };

  const addFloatingEmoji = (e) => {
    const emoji = ["✨", "💫", "⭐", "🌟", "💖", "🎉"][Math.floor(Math.random() * 6)];
    const newEmoji = {
      id: Date.now(),
      emoji,
      x: e.clientX,
      y: e.clientY,
      rotation: Math.random() * 360
    };
    setFloatingEmojis(prev => [...prev, newEmoji]);
    setTimeout(() => {
      setFloatingEmojis(prev => prev.filter(item => item.id !== newEmoji.id));
    }, 1000);
  };

  const handleLogout = () => {
    clearAuthSession();
    navigate("/login");
  };

  return (
    <div className="app-shell" onClick={addFloatingEmoji}>
      {floatingEmojis.map(item => (
        <div
          key={item.id}
          className="floating-emoji"
          style={{
            left: item.x,
            top: item.y,
            transform: `rotate(${item.rotation}deg)`
          }}
        >
          {item.emoji}
        </div>
      ))}
      
      <aside className={`app-sidebar ${sidebarVisible ? '' : 'sidebar-hidden'}`}>
        <div className="app-sidebar-brand">
          <div className="brand-mark gradient-bg animate-gradient pulse-animation">AI</div>
          <div>
            <h2 className="gradient-text">AI Studio</h2>
            <p>Enterprise workspace 🚀</p>
          </div>
        </div>

        <div className="user-avatar-container">
          <div className="user-avatar gradient-bg animate-gradient bounce-animation">
            {avatarEmoji}
          </div>
          <div className="user-info">
            <span className="user-name">Welcome back! 👋</span>
            <span className="user-status">Online 🟢</span>
          </div>
        </div>

        <button
          type="button"
          className="theme-toggle-btn btn btn-secondary"
          onClick={toggleTheme}
        >
          {theme === 'light' ? '🌙 Dark Mode' : '☀️ Light Mode'}
        </button>

        <div className="app-sidebar-stats">
          <span className="stat-pill stat-pro badge badge-primary animate-pulse">PRO ⭐</span>
          <span className="stat-pill stat-credits badge badge-success shimmer-animation">
            ⚡ {loading ? "…" : credits} credits 💎
          </span>
        </div>

        {workspaces.length > 1 && (
          <div className="workspace-selector-wrapper">
            <label className="workspace-label">🏢 Workspace</label>
            <select
              className="workspace-select input"
              value={workspaceId || ""}
              onChange={(e) => switchWorkspace(e.target.value)}
            >
              {workspaces.map((ws) => (
                <option key={ws.id} value={ws.id}>
                  📁 {ws.name}
                </option>
              ))}
            </select>
          </div>
        )}

        {isAdmin() && (
          <button type="button" className="admin-panel-link btn btn-secondary" onClick={() => navigate("/admin")}>
            ⚙ Admin console 🔐
          </button>
        )}

        <nav className="app-sidebar-nav">
          {NAV.map((item) => (
            <button
              key={item.path}
              type="button"
              className={`nav-link ${location.pathname === item.path ? "active" : ""}`}
              onClick={() => navigate(item.path)}
              style={location.pathname === item.path ? { background: item.gradient } : {}}
            >
              <span className="nav-icon">{item.icon}</span>
              <span className="nav-label">{item.label}</span>
              <span className="nav-emoji">{item.emoji}</span>
              {item.path === "/notifications" && unreadCount > 0 && (
                <span className="nav-badge badge badge-danger animate-bounce">{unreadCount}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="app-sidebar-footer">
          <button type="button" className="btn-logout btn btn-danger" onClick={handleLogout}>
            👋 Log out 🚪
          </button>
        </div>
      </aside>

      <main className={`app-main ${sidebarVisible ? '' : 'main-expanded'}`}>
        <header className="app-page-header">
          <div className="header-left">
            <button
              type="button"
              className="sidebar-toggle-btn"
              onClick={toggleSidebar}
              title={sidebarVisible ? "Hide sidebar" : "Show sidebar"}
            >
              {sidebarVisible ? "☰" : "☰"}
            </button>
            <div>
              <h1 className="gradient-text">{title}</h1>
              {subtitle && <p className="subtitle-emoji">{subtitle}</p>}
            </div>
          </div>
          {actions && <div className="app-page-actions">{actions}</div>}
        </header>
        <div className="app-page-body">{children}</div>
      </main>
    </div>
  );
}
