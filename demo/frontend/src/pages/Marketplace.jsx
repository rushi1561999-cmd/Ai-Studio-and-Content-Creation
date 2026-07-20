import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosConfig";
import AppLayout from "../components/AppLayout";
import Icon from "../components/Icon";
import "./Marketplace.css";

export default function Marketplace() {
  const navigate = useNavigate();
  const [posts, setPosts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [publishText, setPublishText] = useState("");
  const [publishing, setPublishing] = useState(false);

  const fetchFeed = async () => {
    try {
      const { data } = await api.get("/marketplace/feed");
      setPosts(data);
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    api.get("/marketplace/feed")
      .then(({ data }) => {
        if (!cancelled) setPosts(data);
      })
      .catch(console.error)
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleLike = async (postId) => {
    try {
      const { data } = await api.post(`/marketplace/${postId}/like`);
      setPosts((prev) => prev.map((p) => (p.id === postId ? data : p)));
    } catch (error) {
      alert(error.response?.data?.message || "Could not like this post.");
    }
  };

  const handlePublish = async (e) => {
    e.preventDefault();
    if (!publishText.trim()) return;
    setPublishing(true);
    try {
      await api.post("/marketplace/publish", {
        promptText: publishText.trim(),
        category: "Community",
      });
      setPublishText("");
      await fetchFeed();
    } catch (error) {
      alert(error.response?.data?.message || "Publish failed.");
    } finally {
      setPublishing(false);
    }
  };

  const handleUsePrompt = (promptText) => {
    navigate("/dashboard", { state: { prompt: promptText } });
  };

  const filteredPosts = posts.filter(
    (post) =>
      post.promptText?.toLowerCase().includes(search.toLowerCase()) ||
      post.authorName?.toLowerCase().includes(search.toLowerCase()) ||
      post.category?.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <AppLayout
      title="Prompt marketplace"
      subtitle="Discover practical prompts from the community and bring them into your own workflow."
      actions={
        <label className="marketplace-search">
          <Icon name="compass" size={18} />
          <input
            type="search"
            className="input marketplace-search-input"
            placeholder="Search prompts or creators"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </label>
      }
    >
      <form className="card publish-form animate-fadeIn" onSubmit={handlePublish}>
        <div className="publish-form-copy">
          <span className="publish-form-icon"><Icon name="sparkles" size={19} /></span>
          <div>
            <h3>Share a useful prompt</h3>
            <p>Help the community start from a stronger idea.</p>
          </div>
        </div>
        <div className="publish-form-entry">
          <textarea
            className="input"
            value={publishText}
            onChange={(e) => setPublishText(e.target.value)}
            placeholder="Paste a prompt that others can reuse…"
            rows={3}
          />
          <button type="submit" className="btn btn-primary" disabled={publishing || !publishText.trim()}>
            {publishing ? "Publishing…" : "Publish prompt"}
          </button>
        </div>
      </form>

      <div className="marketplace-grid">
        {isLoading ? (
          <div className="loading-container animate-shimmer">
            <div className="loading-card card" />
            <div className="loading-card card" />
            <div className="loading-card card" />
          </div>
        ) : filteredPosts.length === 0 ? (
          <div className="card empty-state animate-fadeIn">
            <div className="empty-icon"><Icon name="compass" size={22} /></div>
            <h3>{search ? "No matching prompts" : "No prompts yet"}</h3>
            <p>{search ? "Try a broader search." : "Publish the first community prompt."}</p>
          </div>
        ) : (
          filteredPosts.map((post) => (
            <article
              key={post.id}
              className="marketplace-card card hover-lift"
            >
              <div className="card-header">
                <div className="author-avatar">
                  {(post.authorName || "AI").split(" ").slice(0, 2).map((part) => part[0]).join("").toUpperCase()}
                </div>
                <span className="author-name">{post.authorName}</span>
                <span className="category-tag badge">{post.category || "Community"}</span>
              </div>
              <p className="prompt-text">{post.promptText}</p>
              <div className="card-footer">
                <button type="button" className="like-btn btn btn-secondary" onClick={() => handleLike(post.id)}>
                  <span aria-hidden="true">♡</span> {post.likes || 0}
                </button>
                <button type="button" className="btn btn-primary btn-sm" onClick={() => handleUsePrompt(post.promptText)}>
                  Use prompt <Icon name="arrowRight" size={15} />
                </button>
              </div>
            </article>
          ))
        )}
      </div>
    </AppLayout>
  );
}
