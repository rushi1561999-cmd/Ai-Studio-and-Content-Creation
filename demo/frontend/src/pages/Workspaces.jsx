import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosConfig";
import AppLayout from "../components/AppLayout";
import Icon from "../components/Icon";
import "./Workspaces.css";

export default function Workspaces() {
  const navigate = useNavigate();
  const [workspaces, setWorkspaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: "" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [activeWorkspaceId, setActiveWorkspaceId] = useState(localStorage.getItem("active_workspace_id"));

  const loadWorkspaces = async () => {
    setLoading(true);
    try {
      const { data } = await api.get("/workspaces");
      setWorkspaces(data);
    } catch (err) {
      setError(err.message || "Failed to load workspaces.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    api.get("/workspaces")
      .then(({ data }) => {
        if (!cancelled) setWorkspaces(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || "Failed to load workspaces.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) return;
    setSaving(true);
    setError("");
    try {
      const { data } = await api.post("/workspaces", { name: form.name.trim() });
      setForm({ name: "" });
      setShowForm(false);
      await loadWorkspaces();
      // Automatically switch to new workspace
      setActiveWorkspaceId(data.id);
      localStorage.setItem("active_workspace_id", data.id);
      navigate("/dashboard");
    } catch (err) {
      setError(err.response?.data?.message || "Could not create workspace.");
    } finally {
      setSaving(false);
    }
  };

  const handleSwitch = (workspaceId) => {
    setActiveWorkspaceId(workspaceId);
    localStorage.setItem("active_workspace_id", workspaceId);
    navigate("/dashboard");
  };

  return (
    <AppLayout
      title="Workspaces"
      subtitle="Separate projects, teams, assets, and usage into focused spaces."
      actions={
        <button type="button" className="btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New workspace"}
        </button>
      }
    >
      {error && <div className="alert alert-error">{error}</div>}

      {showForm && (
        <form className="card workspace-form animate-fadeIn" onSubmit={handleCreate}>
          <h3>Create new workspace</h3>
          <label>
            Workspace name
            <input
              value={form.name}
              onChange={(e) => setForm({ name: e.target.value })}
              required
              placeholder="e.g. My Creative Studio"
            />
          </label>
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? "Creating…" : "Create workspace"}
          </button>
        </form>
      )}

      {loading ? (
        <div className="card empty-state animate-fadeIn">
          <div className="empty-icon"><span className="spinner" /></div>
          <h3>Loading workspaces…</h3>
        </div>
      ) : workspaces.length === 0 ? (
        <div className="card empty-state animate-fadeIn">
          <div className="empty-icon"><Icon name="building" size={22} /></div>
          <h3>No workspaces yet</h3>
          <p>Create your first workspace to start organizing content.</p>
        </div>
      ) : (
        <div className="workspace-grid">
          {workspaces.map((ws) => (
            <article
              key={ws.id}
              className={`workspace-card card hover-lift ${activeWorkspaceId === ws.id ? "active" : ""}`}
            >
              <div className="workspace-card-header">
                <span className="workspace-icon"><Icon name="building" size={20} /></span>
                {activeWorkspaceId === ws.id && <span className="active-badge badge badge-success">Active</span>}
              </div>
              <h3>{ws.name}</h3>
              <p className="workspace-meta">Created {formatDate(ws.createdAt)}</p>
              <div className="workspace-actions">
                <button
                  type="button"
                  className={`btn ${activeWorkspaceId === ws.id ? "btn-success" : "btn-primary"}`}
                  onClick={() => handleSwitch(ws.id)}
                >
                  {activeWorkspaceId === ws.id ? "Current workspace" : "Open workspace"}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </AppLayout>
  );
}

function formatDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}
