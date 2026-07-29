import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import api from "./api/axiosConfig";
import MediaPreview from "./components/MediaPreview";
import PromptAssistant from "./components/PromptAssistant";
import VoiceAssistant from "./components/VoiceAssistant";
import "./AiGenerator.css";

const CONTENT_TYPES = [
  { type: "TEXT", icon: "Aa", label: "Text" },
  { type: "IMAGE", icon: "Im", label: "Image" },
  { type: "VIDEO", icon: "Vi", label: "Video" },
  { type: "MIXED", icon: "Mx", label: "Mixed" },
];

const TERMINAL_STATUSES = new Set(["COMPLETED", "FAILED"]);

export default function AiGenerator({
  workspaceId,
  onGenerationSuccess,
  initialPrompt = "",
  savedPrompts = [],
}) {
  const [promptText, setPromptText] = useState(initialPrompt);
  const [contentType, setContentType] = useState("TEXT");
  const [modelKey, setModelKey] = useState("");
  const [jobId, setJobId] = useState(null);
  const [status, setStatus] = useState("IDLE");
  const [result, setResult] = useState("");
  const [mediaUrl, setMediaUrl] = useState("");
  const [activeJob, setActiveJob] = useState(null);
  const onSuccessRef = useRef(onGenerationSuccess);

  useEffect(() => {
    onSuccessRef.current = onGenerationSuccess;
  }, [onGenerationSuccess]);

  const { data: contentTypes = [] } = useQuery({
    queryKey: ["content-types"],
    queryFn: async () => (await api.get("/ai/content-types")).data,
    staleTime: 30 * 60 * 1000,
  });

  const { data: models = [] } = useQuery({
    queryKey: ["ai-models", contentType],
    queryFn: async () =>
      (await api.get(`/ai-models?contentType=${contentType}`)).data,
    staleTime: 5 * 60 * 1000,
  });

  const selectedModelKey = models.some((model) => model.modelKey === modelKey)
    ? modelKey
    : models[0]?.modelKey || "";

  useEffect(() => {
    const handler = (event) => setPromptText(event.detail || "");
    window.addEventListener("ai-studio:set-prompt", handler);
    return () => window.removeEventListener("ai-studio:set-prompt", handler);
  }, []);

  const creditCost = useMemo(
    () =>
      contentTypes.find((content) => content.type === contentType)?.creditCost ??
      (contentType === "VIDEO"
        ? 10
        : contentType === "IMAGE"
          ? 3
          : contentType === "MIXED"
            ? 5
            : 1),
    [contentType, contentTypes],
  );

  const placeholders = {
    TEXT: "Write a blog post about sustainable energy…",
    IMAGE: "A futuristic city at sunset, cinematic lighting, 4k…",
    VIDEO: "Ocean waves on a beach at golden hour, slow motion…",
    MIXED: "Launch campaign for an AI productivity app…",
  };

  const isGenerating = status === "PENDING" || status === "PROCESSING";

  const handleGenerate = async () => {
    const trimmed = promptText.trim();
    if (!trimmed || trimmed.length > 4000) return;

    setStatus("PENDING");
    setResult("");
    setMediaUrl("");
    setJobId(null);
    setActiveJob(null);

    try {
      const { data } = await api.post("/ai/generate", {
        promptText: trimmed,
        workspaceId,
        contentType,
        modelKey: selectedModelKey || undefined,
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
            error.response?.data?.detail ||
            error.message ||
            "Generation failed.",
        );
      }
    }
  };

  useEffect(() => {
    if (!jobId || !workspaceId) return undefined;
    const controller = new AbortController();

    const applyJobUpdate = (job) => {
      if (!job || job.jobId !== jobId) return;
      setStatus(job.status);
      setResult(job.result || "");
      setMediaUrl(job.mediaUrl || "");
      setActiveJob((current) => ({
        ...current,
        id: job.jobId,
        status: job.status,
        result: job.result,
        mediaUrl: job.mediaUrl,
        contentType: job.contentType,
      }));
      if (TERMINAL_STATUSES.has(job.status)) {
        controller.abort();
        onSuccessRef.current?.();
      }
    };

    const connect = async () => {
      const token = localStorage.getItem("jwt_token");
      const baseUrl = String(api.defaults.baseURL || "/api").replace(/\/$/, "");
      const url = `${baseUrl}/ai/jobs/stream/${encodeURIComponent(
        workspaceId,
      )}?jobId=${encodeURIComponent(jobId)}`;

      try {
        const response = await fetch(url, {
          headers: { Authorization: `Bearer ${token}` },
          signal: controller.signal,
        });
        if (!response.ok || !response.body) {
          throw new Error(`Live update connection failed (${response.status}).`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (!controller.signal.aborted) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const blocks = buffer.split(/\r?\n\r?\n/);
          buffer = blocks.pop() || "";

          blocks.forEach((block) => {
            const lines = block.split(/\r?\n/);
            const eventName =
              lines.find((line) => line.startsWith("event:"))?.slice(6).trim() ||
              "message";
            if (eventName !== "job-status") return;
            const payload = lines
              .filter((line) => line.startsWith("data:"))
              .map((line) => line.slice(5).trim())
              .join("\n");
            if (payload) applyJobUpdate(JSON.parse(payload));
          });
        }
      } catch (streamError) {
        if (controller.signal.aborted) return;
        try {
          const { data } = await api.get(`/ai/jobs/${jobId}`, {
            signal: controller.signal,
          });
          applyJobUpdate({ ...data, jobId: data.id });
          if (!TERMINAL_STATUSES.has(data.status)) {
            setResult("Live progress connection was interrupted. Reopen this job from recent creations.");
          }
        } catch (statusError) {
          if (!controller.signal.aborted) {
            setStatus("ERROR");
            setResult(statusError.message || streamError.message);
          }
        }
      }
    };

    connect();
    return () => controller.abort();
  }, [jobId, workspaceId]);

  return (
    <section className="card ai-generator">
      <div className="ai-generator-header">
        <div>
          <p>Generation workspace</p>
          <h2>What will you create?</h2>
        </div>
        <span className="model-badge">
          {creditCost} {creditCost === 1 ? "credit" : "credits"}
        </span>
      </div>

      <div className="content-type-tabs">
        {CONTENT_TYPES.map((type) => (
          <button
            key={type.type}
            type="button"
            className={`type-tab ${contentType === type.type ? "active" : ""}`}
            onClick={() => setContentType(type.type)}
            disabled={isGenerating}
          >
            <span aria-hidden="true">{type.icon}</span>
            {type.label}
          </button>
        ))}
      </div>

      {contentTypes.find((content) => content.type === contentType)?.description && (
        <p className="type-hint">
          {contentTypes.find((content) => content.type === contentType).description}
        </p>
      )}

      {models.length > 0 && (
        <label className="model-select-label">
          Model
          <select
            value={selectedModelKey}
            onChange={(event) => setModelKey(event.target.value)}
            disabled={isGenerating}
          >
            {models.map((model) => (
              <option key={model.id} value={model.modelKey}>
                {model.displayName}
              </option>
            ))}
          </select>
        </label>
      )}

      {savedPrompts.length > 0 && (
        <div className="quick-prompts">
          <span>Quick insert:</span>
          {savedPrompts.slice(0, 3).map((prompt) => (
            <button
              key={prompt.id}
              type="button"
              className="chip"
              onClick={() => setPromptText(prompt.content)}
            >
              {prompt.title}
            </button>
          ))}
        </div>
      )}

      <VoiceAssistant
        onTranscript={(text) =>
          setPromptText((current) => current + (current ? " " : "") + text)
        }
        onSpeak={() => {
          if (result && status === "COMPLETED") {
            window.speechSynthesis.speak(new SpeechSynthesisUtterance(result));
          }
        }}
        disabled={isGenerating}
      />

      <div className="input-section">
        <textarea
          placeholder={placeholders[contentType]}
          value={promptText}
          onChange={(event) => setPromptText(event.target.value)}
          disabled={isGenerating}
          maxLength={4000}
          rows={6}
        />
        <span className="prompt-character-count">{promptText.length}/4000</span>

        <PromptAssistant
          workspaceId={workspaceId}
          promptText={promptText}
          contentType={contentType}
          onApply={setPromptText}
          disabled={isGenerating}
        />

        <button
          type="button"
          className="btn-primary btn-generate"
          onClick={handleGenerate}
          disabled={isGenerating || !promptText.trim()}
        >
          {isGenerating
            ? contentType === "VIDEO"
              ? "Generating video… (may take 1–2 min)"
              : status === "PROCESSING"
                ? "Generating…"
                : "Queued…"
            : `Generate ${contentType.toLowerCase()} (${creditCost} credits)`}
        </button>
      </div>

      {isGenerating && (
        <div className="loading-state">
          <div className="spinner" />
          <p>
            {contentType === "VIDEO"
              ? "Rendering video — this can take a few minutes…"
              : contentType === "IMAGE"
                ? "Creating your image…"
                : status === "PENDING"
                  ? "Your job is queued…"
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
    </section>
  );
}
