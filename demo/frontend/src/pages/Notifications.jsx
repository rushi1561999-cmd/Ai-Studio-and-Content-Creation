import React, { useEffect, useState } from "react";
import AppLayout from "../components/AppLayout";
import { useWorkspace } from "../context/WorkspaceContext";
import api from "../api/axiosConfig";
import "./Notifications.css";

const NOTIFICATION_COLORS = {
  BILLING: "var(--warning-gradient)",
  GENERATION: "var(--primary-gradient)",
  MARKETPLACE: "var(--accent-gradient)",
  SYSTEM: "var(--dark-gradient)",
  WORKSPACE: "var(--success-gradient)",
};

const NOTIFICATION_EMOJIS = {
  BILLING: "💳",
  GENERATION: "✨",
  MARKETPLACE: "🛍️",
  SYSTEM: "⚙️",
  WORKSPACE: "🏢",
};

const AVATAR_EMOJIS = ["🎨", "🚀", "💡", "🌟", "🎪", "🎭", "🎬", "🎮", "🎲", "🎯"];

export default function Notifications() {
  const { refreshUnread } = useWorkspace();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await api.get("/notifications");
      setItems(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const markRead = async (id) => {
    try {
      const response = await api.patch(`/notifications/${id}/read`);
      console.log('Marked as read:', response.data);
      await load();
      refreshUnread();
    } catch (err) {
      console.error('Error marking notification as read:', err);
      alert('Failed to mark notification as read. Please try again.');
    }
  };

  const markAllRead = async () => {
    try {
      await api.patch("/notifications/read-all");
      await load();
      refreshUnread();
    } catch (err) {
      console.error('Error marking all notifications as read:', err);
      alert('Failed to mark all notifications as read. Please try again.');
    }
  };

  return (
    <AppLayout
      title="Notifications 🔔"
      subtitle="Generation updates, billing, and workspace activity ✨"
      actions={
        items.some((n) => !n.read) ? (
          <button type="button" className="btn btn-primary" onClick={markAllRead}>
            ✅ Mark all read
          </button>
        ) : null
      }
    >
      {loading ? (
        <div className="loading-container animate-shimmer">
          <div className="loading-card card" />
          <div className="loading-card card" />
          <div className="loading-card card" />
        </div>
      ) : items.length === 0 ? (
        <div className="card empty-state animate-fadeIn">
          <div className="empty-icon pulse-animation">🎉</div>
          <h3>You're all caught up! 🌟</h3>
          <p>No notifications yet. Enjoy your day! ✨</p>
        </div>
      ) : (
        <div className="notification-grid">
          {items.map((n, index) => (
            <div
              key={n.id}
              className={`notification-card card ${n.read ? "read" : "unread"} hover-lift`}
              style={{ animationDelay: `${index * 0.1}s` }}
            >
              <div className="notification-header">
                <div className="notification-avatar">
                  {AVATAR_EMOJIS[index % AVATAR_EMOJIS.length]}
                </div>
                <span
                  className="type-badge"
                  style={{ background: NOTIFICATION_COLORS[n.type] || NOTIFICATION_COLORS.SYSTEM }}
                >
                  {NOTIFICATION_EMOJIS[n.type] || "⚙️"} {n.type || "SYSTEM"}
                </span>
                <time className="notification-time">{formatDate(n.createdAt)}</time>
              </div>
              <h3 className="notification-title">{n.title}</h3>
              <p className="notification-message">{n.message}</p>
              {!n.read && (
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => markRead(n.id)}
                >
                  ✅ Mark as read
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </AppLayout>
  );
}

function formatDate(iso) {
  if (!iso) return "";
  const date = new Date(iso);
  const now = new Date();
  const diff = now - date;
  
  if (diff < 60000) return "Just now";
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}d ago`;
  
  return date.toLocaleDateString();
}
