import { useEffect, useState } from "react";
import AppLayout from "../components/AppLayout";
import { useWorkspace } from "../context/workspace-context";
import api from "../api/axiosConfig";
import Icon from "../components/Icon";
import "./Notifications.css";

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
    let cancelled = false;
    api.get("/notifications")
      .then(({ data }) => {
        if (!cancelled) setItems(data);
      })
      .catch(console.error)
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
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
      title="Notifications"
      subtitle="Generation updates, billing events, and workspace activity in one feed."
      actions={
        items.some((n) => !n.read) ? (
          <button type="button" className="btn btn-primary" onClick={markAllRead}>
            Mark all as read
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
          <div className="empty-icon"><Icon name="bell" size={22} /></div>
          <h3>You&apos;re all caught up</h3>
          <p>New workspace activity will appear here.</p>
        </div>
      ) : (
        <div className="notification-grid">
          {items.map((n) => (
            <div
              key={n.id}
              className={`notification-card card ${n.read ? "read" : "unread"} hover-lift`}
            >
              <div className="notification-header">
                <div className="notification-avatar">
                  <Icon name={n.type === "BILLING" ? "wallet" : n.type === "WORKSPACE" ? "building" : "bell"} size={18} />
                </div>
                <span className="type-badge">{n.type || "SYSTEM"}</span>
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
                  Mark as read
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
