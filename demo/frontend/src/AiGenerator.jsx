import React, { useState, useEffect } from "react";
import api from "./api/axiosConfig";
import MediaPreview from "./components/MediaPreview";
import VoiceAssistant from "./components/VoiceAssistant";
import "./AiGenerator.css";

const CONTENT_TYPES = [
  { type: "TEXT", icon: "📝", label: "Text" },
  { type: "IMAGE", icon: "🖼️", label: "Image" },
  { type: "VIDEO", icon: "🎬", label: "Video" },
  { type: "MIXED", icon: "✨", label: "Rich" },
];

export default function AiGenerator({
  workspaceId,
  onGenerationSuccess,
  initialPrompt = "",
  savedPrompts = [],
}) {
  const [promptText, setPromptText] = useState(initialPrompt);
  const [contentType, setContentType] = useState("TEXT");
  const [contentTypes, setContentTypes] = useState([]);
  const [models, setModels] = useState([]);
  const [modelKey, setModelKey] = useState("");
  const [jobId, setJobId] = useState(null);
  const [status, setStatus] = useState("IDLE");
  const [result, setResult] = useState("");
  const [mediaUrl, setMediaUrl] = useState("");
  const [activeJob, setActiveJob] = useState(null);

  useEffect(() => {
    api
      .get("/ai/content-types")
      .then((res) => setContentTypes(res.data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    api
      .get(`/ai-models?contentType=${contentType}`)
      .then((res) => {
        setModels(res.data);
        setModelKey(res.data[0]?.modelKey || "");
      })
      .catch(() => setModels([]));
  }, [contentType]);

  useEffect(() => {
    if (initialPrompt) setPromptText(initialPrompt);
  }, [initialPrompt]);

  useEffect(() => {
    const handler = (e) => setPromptText(e.detail || "");
    window.addEventListener("ai-studio:set-prompt", handler);
    return () => window.removeEventListener("ai-studio:set-prompt", handler);
  }, []);

  const creditCost =
    contentTypes.find((c) => c.type === contentType)?.creditCost ??
    (contentType === "VIDEO"
      ? 10
      : contentType === "IMAGE"
        ? 3
        : contentType === "MIXED"
          ? 5
          : 1);

  const placeholders = {
    TEXT: "Write a blog post about sustainable energy…",
    IMAGE: "A futuristic city at sunset, cinematic lighting, 4k…",
    VIDEO: "Ocean waves on a beach at golden hour, slow motion…",
    MIXED: "Launch campaign for an AI productivity app…",
  };

  const handleGenerate = async () => {
    if (!promptText.trim()) return;

    setStatus("PENDING");
    setResult("");
    setMediaUrl("");
    setJobId(null);
    setActiveJob(null);

    try {
      const { data } = await api.post("/ai/generate", {
        promptText: promptText.trim(),
        workspaceId,
        contentType,
        modelKey: modelKey || undefined,
      });
      setJobId(data.id);
      setActiveJob(data);
    } catch (error) {
      if (error.response?.status === 402) {
        setStatus("FAILED");
        setResult(
          typeof error.response.data === "string"
            ? error.response.data
            : `Insufficient credits. Need ${creditCost} credits.`,
        );
      } else {
        setStatus("ERROR");
        setResult(
          error.response?.data?.message ||
            error.message ||
            "Generation failed.",
        );
      }
    }
  };

  useEffect(() => {
    let intervalId;

    if (jobId && status === "PENDING") {
      const pollMs = contentType === "VIDEO" ? 4000 : 2000;
      intervalId = setInterval(async () => {
        try {
          const { data } = await api.get(`/ai/jobs/${jobId}`);
          if (data.status === "COMPLETED" || data.status === "FAILED") {
            setResult(data.result || "");
            setMediaUrl(data.mediaUrl || "");
            setStatus(data.status);
            setActiveJob(data);
            clearInterval(intervalId);
            if (onGenerationSuccess) onGenerationSuccess();
          }
        } catch (error) {
          console.error(error);
        }
      }, pollMs);
    }

    return () => clearInterval(intervalId);
  }, [jobId, status, onGenerationSuccess, contentType]);

  return (
    <div className="card ai-generator">
      <div className="ai-generator-header">
        <h2>AI Content Studio</h2>
        <span className="model-badge">{creditCost} credits</span>
      </div>

      <div className="content-type-tabs">
        {CONTENT_TYPES.map((t) => (
          <button
            key={t.type}
            type="button"
            className={`type-tab ${contentType === t.type ? "active" : ""}`}
            onClick={() => setContentType(t.type)}
            disabled={status === "PENDING"}
          >
            <span>{t.icon}</span>
            {t.label}
          </button>
        ))}
      </div>

      {contentTypes.find((c) => c.type === contentType)?.description && (
        <p className="type-hint">
          {contentTypes.find((c) => c.type === contentType).description}
        </p>
      )}

      {models.length > 0 && (
        <label className="model-select-label">
          Model
          <select
            value={modelKey}
            onChange={(e) => setModelKey(e.target.value)}
            disabled={status === "PENDING"}
          >
            {models.map((m) => (
              <option key={m.id} value={m.modelKey}>
                {m.displayName}
              </option>
            ))}
          </select>
        </label>
      )}

      {savedPrompts.length > 0 && (
        <div className="quick-prompts">
          <span>Quick insert:</span>
          {savedPrompts.slice(0, 3).map((p) => (
            <button
              key={p.id}
              type="button"
              className="chip"
              onClick={() => setPromptText(p.content)}
            >
              {p.title}
            </button>
          ))}
        </div>
      )}

      <VoiceAssistant
        onTranscript={(text) => setPromptText(prev => prev + (prev ? " " : "") + text)}
        onSpeak={() => {
          if (result && status === "COMPLETED") {
            const utterance = new SpeechSynthesisUtterance(result);
            window.speechSynthesis.speak(utterance);
          }
        }}
        disabled={status === "PENDING"}
      />

      <div className="input-section">
        <textarea
          placeholder={placeholders[contentType]}
          value={promptText}
          onChange={(e) => setPromptText(e.target.value)}
          disabled={status === "PENDING"}
          rows={6}
        />
        <button
          type="button"
          className="btn-primary btn-generate"
          onClick={handleGenerate}
          disabled={status === "PENDING" || !promptText.trim()}
        >
          {status === "PENDING"
            ? contentType === "VIDEO"
              ? "Generating video… (may take 1–2 min)"
              : "Generating…"
            : `Generate ${contentType.toLowerCase()} (${creditCost} credits)`}
        </button>
      </div>

      {status === "PENDING" && (
        <div className="loading-state">
          <div className="spinner" />
          <p>
            {contentType === "VIDEO"
              ? "Rendering video — this can take a few minutes…"
              : contentType === "IMAGE"
                ? "Creating your image…"
                : "AI is working…"}
          </p>
        </div>
      )}

      {(status === "COMPLETED" || status === "FAILED" || status === "ERROR") &&
        (result || mediaUrl) && (
          <div
            className={`result-section ${status !== "COMPLETED" ? "error-result" : ""}`}
          >
            <h3>
              {status === "COMPLETED"
                ? `${contentType.charAt(0) + contentType.slice(1).toLowerCase()} output`
                : "Error"}
            </h3>
            <MediaPreview
              contentType={activeJob?.contentType || contentType}
              result={result}
              mediaUrl={mediaUrl}
              status={status}
            />
          </div>
        )}
    </div>
  );
}
