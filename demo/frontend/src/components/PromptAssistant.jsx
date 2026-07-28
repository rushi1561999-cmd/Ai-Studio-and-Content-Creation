import { memo, useEffect, useRef, useState } from "react";
import api from "../api/axiosConfig";
import "./PromptAssistant.css";

const MIN_PROMPT_LENGTH = 20;
const DEBOUNCE_MILLIS = 750;

function PromptAssistant({
  workspaceId,
  promptText,
  contentType,
  onApply,
  disabled = false,
}) {
  const [assistant, setAssistant] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const lastRequestRef = useRef("");

  useEffect(() => {
    const trimmed = promptText.trim();
    if (
      disabled ||
      !workspaceId ||
      trimmed.length < MIN_PROMPT_LENGTH ||
      trimmed.length > 4000
    ) {
      return undefined;
    }

    const requestKey = `${contentType}:${trimmed}`;
    if (requestKey === lastRequestRef.current) return undefined;

    const controller = new AbortController();
    const timer = window.setTimeout(async () => {
      setLoading(true);
      setError("");
      try {
        const { data } = await api.post(
          "/prompt-assistant/suggestions",
          {
            workspaceId,
            promptText: trimmed,
            contentType,
            variantCount: 3,
          },
          { signal: controller.signal },
        );
        lastRequestRef.current = requestKey;
        setAssistant(data);
      } catch (requestError) {
        if (
          requestError.code !== "ERR_CANCELED" &&
          requestError.name !== "CanceledError"
        ) {
          setError(
            requestError.response?.data?.message ||
              requestError.response?.data?.detail ||
              "Suggestions are temporarily unavailable.",
          );
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }, DEBOUNCE_MILLIS);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [contentType, disabled, promptText, workspaceId]);

  if (promptText.trim().length < MIN_PROMPT_LENGTH) {
    return (
      <p className="prompt-assistant-hint">
        Write at least {MIN_PROMPT_LENGTH} characters to receive automatic prompt
        suggestions.
      </p>
    );
  }

  return (
    <section className="prompt-assistant" aria-live="polite">
      <div className="prompt-assistant-heading">
        <div>
          <span className="assistant-kicker">Prompt assistant</span>
          <strong>
            {loading
              ? "Improving your prompt…"
              : assistant
                ? `Quality ${assistant.qualityScore}/100`
                : "Waiting for a complete idea"}
          </strong>
        </div>
        {assistant?.source && (
          <span className="assistant-source">
            {assistant.source === "AI" ? "AI enhanced" : "Smart fallback"}
          </span>
        )}
      </div>

      {loading && <div className="assistant-progress" />}
      {error && <p className="assistant-error">{error}</p>}

      {assistant && !loading && (
        <>
          <div className="assistant-optimized">
            <div>
              <strong>Recommended prompt</strong>
              <button
                type="button"
                className="btn-secondary btn-sm"
                onClick={() => onApply(assistant.optimizedPrompt)}
              >
                Use this prompt
              </button>
            </div>
            <p>{assistant.optimizedPrompt}</p>
          </div>

          {assistant.suggestions?.length > 0 && (
            <ul className="assistant-suggestions">
              {assistant.suggestions.slice(0, 4).map((suggestion) => (
                <li key={suggestion}>{suggestion}</li>
              ))}
            </ul>
          )}

          {assistant.variants?.length > 0 && (
            <div className="assistant-variants">
              {assistant.variants.map((variant) => (
                <button
                  type="button"
                  key={`${variant.label}-${variant.prompt}`}
                  onClick={() => onApply(variant.prompt)}
                >
                  <strong>{variant.label}</strong>
                  <span>{variant.reason}</span>
                </button>
              ))}
            </div>
          )}
        </>
      )}
    </section>
  );
}

export default memo(PromptAssistant);
