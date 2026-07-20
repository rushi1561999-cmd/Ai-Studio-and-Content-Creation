import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import api from "../api/axiosConfig";
import AppLayout from "../components/AppLayout";
import { useWorkspace } from "../context/workspace-context";
import AiGenerator from "../AiGenerator";
import MediaPreview from "../components/MediaPreview";
import Icon from "../components/Icon";
import "./Dashboard.css";

const TYPE_LABELS = { TEXT: "Text", IMAGE: "Image", VIDEO: "Video", MIXED: "Mixed" };

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
    if (!workspaceId) return undefined;
    let cancelled = false;
    Promise.all([
      api.get(`/ai/jobs/workspace/${workspaceId}`),
      api.get(`/ai/contents/workspace/${workspaceId}`),
      api.get(`/prompts/workspace/${workspaceId}`),
    ])
      .then(([jobsRes, contentsRes, promptsRes]) => {
        if (cancelled) return;
        setHistory(jobsRes.data);
        setContents(contentsRes.data);
        setPrompts(promptsRes.data);
      })
      .catch(console.error);
    return () => {
      cancelled = true;
    };
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

  const applySavedPrompt = (content) => {
    window.dispatchEvent(
      new CustomEvent("ai-studio:set-prompt", { detail: content }),
    );
  };

  return (
    <AppLayout
      title="Create content"
      subtitle="Turn a prompt into polished text, images, video, or a complete creative concept."
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
              <div className="empty-icon"><span className="spinner" /></div>
              <h3>Preparing your workspace…</h3>
            </div>
          )}
        </div>

        <aside className="history-column">
          {prompts.length > 0 && (
            <section className="sidebar-section animate-fadeIn">
              <div className="section-heading">
                <h2>Saved prompts</h2>
                <span>{prompts.length}</span>
              </div>
              <ul className="saved-prompt-list">
                {prompts.slice(0, 5).map((p) => (
                  <li key={p.id}>
                    <button
                      type="button"
                      className="prompt-card card hover-lift"
                      onClick={() => applySavedPrompt(p.content)}
                    >
                      <span className="prompt-emoji"><Icon name="sparkles" size={16} /></span>
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

          <div className="section-heading history-heading">
            <h2>Recent creations</h2>
            <span>{history.length}</span>
          </div>
          <div className="history-list">
            {history.length === 0 ? (
              <div className="card empty-state animate-fadeIn">
                <div className="empty-icon"><Icon name="sparkles" size={22} /></div>
                <h3>No creations yet</h3>
                <p>Your latest generated content will appear here.</p>
              </div>
            ) : (
              history.map((job) => (
                <article
                  key={job.id}
                  className="history-card card hover-lift"
                >
                  <div className="history-card-top">
                    <span
                      className="content-type-pill"
                    >
                      {TYPE_LABELS[job.contentType] || "Text"}
                    </span>
                    <span className={`status-badge status-${job.status.toLowerCase()}`}>
                      {job.status}
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
              <div className="section-heading"><h2>Media gallery</h2></div>
              <div className="gallery-grid">
                {contents
                  .filter((c) => c.mediaUrl || c.contentType === "IMAGE" || c.contentType === "VIDEO")
                  .slice(0, 6)
                  .map((c) => (
                    <div
                      key={c.id}
                      className="gallery-item card hover-lift"
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
