import React, { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";
import api from "../api/axiosConfig";
import AppLayout from "../components/AppLayout";
import { useWorkspace } from "../context/WorkspaceContext";
import AiGenerator from "../AiGenerator";
import MediaPreview from "../components/MediaPreview";
import "./Dashboard.css";

const TYPE_ICONS = { TEXT: "📝", IMAGE: "🖼️", VIDEO: "🎬", MIXED: "✨" };
const TYPE_COLORS = {
  TEXT: "var(--primary-gradient)",
  IMAGE: "var(--accent-gradient)",
  VIDEO: "var(--secondary-gradient)",
  MIXED: "var(--warning-gradient)",
};
const AVATAR_EMOJIS = ["🎨", "🚀", "💡", "🌟", "🎪", "🎭", "🎬", "🎮", "🎲", "🎯"];

export default function Dashboard() {
  const location = useLocation();
  const importedPrompt = location.state?.prompt || "";
  const { workspaceId, refreshWallet, refreshUnread } = useWorkspace();

  const [history, setHistory] = useState([]);
  const [contents, setContents] = useState([]);
  const [prompts, setPrompts] = useState([]);
  const [expandedJobId, setExpandedJobId] = useState(null);

  const fetchHistory = async (wsId) => {
    const [jobsRes, contentsRes, promptsRes] = await Promise.all([
      api.get(`/ai/jobs/workspace/${wsId}`),
      api.get(`/ai/contents/workspace/${wsId}`),
      api.get(`/prompts/workspace/${wsId}`),
    ]);
    setHistory(jobsRes.data);
    setContents(contentsRes.data);
    setPrompts(promptsRes.data);
  };

  useEffect(() => {
    if (workspaceId) {
      fetchHistory(workspaceId).catch(console.error);
    }
  }, [workspaceId]);

  const handlePublish = async (job) => {
    try {
      await api.post("/marketplace/publish", {
        promptText: job.promptText,
        category: job.contentType || "Community",
      });
      alert("Published to the Marketplace.");
    } catch {
      alert("Failed to publish.");
    }
  };

  const onGenerationSuccess = () => {
    if (workspaceId) {
      fetchHistory(workspaceId);
      refreshWallet();
      refreshUnread();
    }
  };

  const useSavedPrompt = (content) => {
    window.dispatchEvent(
      new CustomEvent("ai-studio:set-prompt", { detail: content }),
    );
  };

  return (
    <AppLayout
      title="Content Studio 🎨"
      subtitle="Create text, images, videos, and rich combined content with AI ✨"
    >
      <div className="dashboard-grid">
        <div className="generator-column">
          {workspaceId ? (
            <AiGenerator
              workspaceId={workspaceId}
              initialPrompt={importedPrompt}
              savedPrompts={prompts}
              onGenerationSuccess={onGenerationSuccess}
            />
          ) : (
            <div className="card empty-state animate-fadeIn">
              <div className="empty-icon bounce-animation">⏳</div>
              <h3>Loading workspace… 🚀</h3>
            </div>
          )}
        </div>

        <aside className="history-column">
          {prompts.length > 0 && (
            <section className="sidebar-section animate-fadeIn">
              <h2 className="gradient-text">Saved prompts 💾</h2>
              <ul className="saved-prompt-list">
                {prompts.slice(0, 5).map((p, index) => (
                  <li key={p.id} style={{ animationDelay: `${index * 0.1}s` }}>
                    <button
                      type="button"
                      className="prompt-card card hover-lift"
                      onClick={() => useSavedPrompt(p.content)}
                    >
                      <span className="prompt-emoji">{AVATAR_EMOJIS[index % AVATAR_EMOJIS.length]}</span>
                      <div className="prompt-content">
                        <strong>{p.title}</strong>
                        <span>{p.content.substring(0, 60)}…</span>
                      </div>
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <h2 className="gradient-text">Recent creations 🎬</h2>
          <div className="history-list">
            {history.length === 0 ? (
              <div className="card empty-state animate-fadeIn">
                <div className="empty-icon pulse-animation">🎨</div>
                <h3>No generations yet 🌟</h3>
                <p>Pick a type above and start creating! ✨</p>
              </div>
            ) : (
              history.map((job, index) => (
                <article
                  key={job.id}
                  className="history-card card hover-lift"
                  style={{ animationDelay: `${index * 0.1}s` }}
                >
                  <div className="history-card-top">
                    <span
                      className="content-type-pill"
                      style={{ background: TYPE_COLORS[job.contentType] || TYPE_COLORS.TEXT }}
                    >
                      {TYPE_ICONS[job.contentType] || "📝"} {job.contentType || "TEXT"}
                    </span>
                    <span className={`status-badge status-${job.status.toLowerCase()}`}>
                      {job.status === "COMPLETED" ? "✅" : job.status === "FAILED" ? "❌" : "⏳"} {job.status}
                    </span>
                  </div>
                  <p className="history-prompt">{job.promptText}</p>

                  {job.status === "COMPLETED" && (
                    <>
                      {(job.mediaUrl || job.contentType === "IMAGE" || job.contentType === "VIDEO") && (
                        <div className="history-thumb-wrap">
                          {job.mediaUrl && job.contentType === "IMAGE" && (
                            <img src={job.mediaUrl} alt="" className="history-thumb" />
                          )}
                          {job.mediaUrl && job.contentType === "VIDEO" && (
                            <video src={job.mediaUrl} className="history-thumb-video" muted />
                          )}
                        </div>
                      )}
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() =>
                          setExpandedJobId(expandedJobId === job.id ? null : job.id)
                        }
                      >
                        {expandedJobId === job.id ? "Hide" : "View"} output
                      </button>
                      {expandedJobId === job.id && (
                        <div className="history-expanded">
                          <MediaPreview
                            contentType={job.contentType}
                            result={job.result}
                            mediaUrl={job.mediaUrl}
                            status={job.status}
                          />
                        </div>
                      )}
                    </>
                  )}

                  <div className="history-card-footer">
                    {job.status === "COMPLETED" && (
                      <button
                        type="button"
                        className="btn btn-success btn-sm"
                        onClick={() => handlePublish(job)}
                      >
                        Publish
                      </button>
                    )}
                  </div>
                </article>
              ))
            )}
          </div>

          {contents.length > 0 && (
            <section className="sidebar-section content-gallery animate-fadeIn">
              <h2 className="gradient-text">Media gallery</h2>
              <div className="gallery-grid">
                {contents
                  .filter((c) => c.mediaUrl || c.contentType === "IMAGE" || c.contentType === "VIDEO")
                  .slice(0, 6)
                  .map((c, index) => (
                    <div
                      key={c.id}
                      className="gallery-item card hover-lift"
                      style={{ animationDelay: `${index * 0.1}s` }}
                    >
                      {c.mediaUrl && c.contentType === "IMAGE" && (
                        <img src={c.mediaUrl} alt="" />
                      )}
                      {c.mediaUrl && c.contentType === "VIDEO" && (
                        <video src={c.mediaUrl} muted />
                      )}
                      <span className="gallery-label badge badge-primary">{c.contentType}</span>
                    </div>
                  ))}
              </div>
            </section>
          )}
        </aside>
      </div>
    </AppLayout>
  );
}
