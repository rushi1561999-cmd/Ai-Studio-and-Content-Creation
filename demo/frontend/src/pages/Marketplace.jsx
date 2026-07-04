import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosConfig";
import AppLayout from "../components/AppLayout";
import "./Marketplace.css";

const CATEGORY_COLORS = {
  COMMUNITY: "var(--primary-gradient)",
  TUTORIAL: "var(--accent-gradient)",
  TEMPLATE: "var(--success-gradient)",
  CREATIVE: "var(--secondary-gradient)",
};

const CATEGORY_EMOJIS = {
  COMMUNITY: "🌍",
  TUTORIAL: "📚",
  TEMPLATE: "📋",
  CREATIVE: "🎨",
};

const AVATAR_EMOJIS = ["🎨", "🚀", "💡", "🌟", "🎪", "🎭", "🎬", "🎮", "🎲", "🎯"];

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
    fetchFeed();
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
      title="Prompt Marketplace 🛍️"
      subtitle="Discover, like, and reuse community prompts ✨"
      actions={
        <input
          type="search"
          className="input marketplace-search-input"
          placeholder="🔍 Search prompts…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      }
    >
      <form className="card publish-form animate-fadeIn" onSubmit={handlePublish}>
        <h3 className="gradient-text">Share a prompt 💡</h3>
        <textarea
          className="input"
          value={publishText}
          onChange={(e) => setPublishText(e.target.value)}
          placeholder="Paste your best prompt for the community… ✨"
          rows={3}
        />
        <button type="submit" className="btn btn-primary" disabled={publishing || !publishText.trim()}>
          {publishing ? "Publishing… 🚀" : "Publish to marketplace 🚀"}
        </button>
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
            <div className="empty-icon pulse-animation">🌍</div>
            <h3>No prompts yet 🎨</h3>
            <p>{search ? "No prompts match your search. 🔍" : "Be the first to publish! 🚀"}</p>
          </div>
        ) : (
          filteredPosts.map((post, index) => (
            <article
              key={post.id}
              className="marketplace-card card hover-lift"
              style={{ animationDelay: `${index * 0.1}s` }}
            >
              <div className="card-header">
                <div className="author-avatar">
                  {AVATAR_EMOJIS[index % AVATAR_EMOJIS.length]}
                </div>
                <span className="author-name">{post.authorName}</span>
                <span
                  className="category-tag badge"
                  style={{ background: CATEGORY_COLORS[post.category?.toUpperCase()] || CATEGORY_COLORS.COMUNITY }}
                >
                  {CATEGORY_EMOJIS[post.category?.toUpperCase()] || "🌍"} {post.category}
                </span>
              </div>
              <p className="prompt-text">"{post.promptText}"</p>
              <div className="card-footer">
                <button type="button" className="like-btn btn btn-secondary" onClick={() => handleLike(post.id)}>
                  ❤️ {post.likes}
                </button>
                <button type="button" className="btn btn-primary btn-sm" onClick={() => handleUsePrompt(post.promptText)}>
                  Use in Studio 🎨
                </button>
              </div>
            </article>
          ))
        )}
      </div>
    </AppLayout>
  );
}
