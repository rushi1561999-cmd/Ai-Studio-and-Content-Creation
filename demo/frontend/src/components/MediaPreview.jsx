import "./MediaPreview.css";

export default function MediaPreview({ contentType, result, mediaUrl, status }) {
  const type = (contentType || "TEXT").toUpperCase();
  const isError = status === "FAILED" || status === "ERROR" || (result && result.startsWith("Error:"));

  if (isError) {
    return <pre className="media-preview-error">{result}</pre>;
  }

  if (type === "IMAGE" || (type === "MIXED" && mediaUrl)) {
    const src = mediaUrl || result;
    if (src && (src.startsWith("http") || src.startsWith("data:"))) {
      return (
        <div className="media-preview-image">
          <img src={src} alt="Generated" />
          <a href={src} target="_blank" rel="noreferrer" className="media-open-link">
            Open full size
          </a>
        </div>
      );
    }
  }

  if (type === "VIDEO" && (mediaUrl || result)) {
    const src = mediaUrl || result;
    if (src && src.startsWith("http")) {
      return (
        <div className="media-preview-video">
          <video controls src={src}>
            Your browser does not support video playback.
          </video>
          <a href={src} target="_blank" rel="noreferrer" className="media-open-link">
            Download video
          </a>
        </div>
      );
    }
  }

  if (type === "MIXED") {
    return (
      <div className="media-preview-mixed">
        {mediaUrl && (
          <img src={mediaUrl} alt="Generated visual" className="mixed-image" />
        )}
        {result && <pre className="media-preview-text">{result}</pre>}
      </div>
    );
  }

  return <pre className="media-preview-text">{result}</pre>;
}
