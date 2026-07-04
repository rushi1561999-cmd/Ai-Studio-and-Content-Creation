import React, { useEffect, useState } from "react";
import AppLayout from "../components/AppLayout";
import { useWorkspace } from "../context/WorkspaceContext";
import api from "../api/axiosConfig";
import "./Prompts.css";

export default function Prompts() {
  const { workspaceId, loading: wsLoading } = useWorkspace();
  const [prompts, setPrompts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    title: "",
    content: "",
    categoryName: "General",
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const loadPrompts = async () => {
    if (!workspaceId) return;
    setLoading(true);
    try {
      const { data } = await api.get(`/prompts/workspace/${workspaceId}`);
      setPrompts(data);
    } catch (err) {
      setError(err.message || "Failed to load prompts.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (workspaceId) loadPrompts();
  }, [workspaceId]);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!workspaceId) return;
    setSaving(true);
    setError("");
    try {
      await api.post("/prompts", { ...form, workspaceId });
      setForm({ title: "", content: "", categoryName: "General" });
      setShowForm(false);
      await loadPrompts();
    } catch (err) {
      setError(err.response?.data?.message || "Could not save prompt.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppLayout
      title="Prompt Library"
      subtitle="Save and organize prompts for your workspace."
      actions={
        <button type="button" className="btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "+ New prompt"}
        </button>
      }
    >
      {error && <div className="alert alert-error">{error}</div>}

      {showForm && (
        <form className="card prompt-form" onSubmit={handleCreate}>
          <h3>Create prompt</h3>
          <label>
            Title
            <input
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              required
              placeholder="e.g. Blog outline generator"
            />
          </label>
          <label>
            Category
            <input
              value={form.categoryName}
              onChange={(e) => setForm({ ...form, categoryName: e.target.value })}
              required
            />
          </label>
          <label>
            Content
            <textarea
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
              required
              rows={5}
              placeholder="The full prompt text…"
            />
          </label>
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? "Saving…" : "Save prompt"}
          </button>
        </form>
      )}

      {wsLoading || loading ? (
        <p className="empty-state">Loading prompts…</p>
      ) : prompts.length === 0 ? (
        <div className="card empty-state">
          <p>No prompts yet. Create one to reuse across generations.</p>
        </div>
      ) : (
        <div className="prompt-grid">
          {prompts.map((p) => (
            <article key={p.id} className="card prompt-card">
              <div className="prompt-card-head">
                <h3>{p.title}</h3>
                <span className="category-chip">{p.categoryName}</span>
              </div>
              <p className="prompt-preview">{p.content}</p>
              <button
                type="button"
                className="btn-secondary btn-sm"
                onClick={() => {
                  navigator.clipboard.writeText(p.content);
                }}
              >
                Copy to clipboard
              </button>
            </article>
          ))}
        </div>
      )}
    </AppLayout>
  );
}
