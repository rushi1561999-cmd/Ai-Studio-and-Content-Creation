import { useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/axiosConfig";
import AuthShell from "../components/AuthShell";
import Icon from "../components/Icon";
import "./Login.css";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [resetUrl, setResetUrl] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");
    setResetUrl("");
    try {
      const { data } = await api.post("/auth/forgot-password", { email });
      setMessage(data.message);
      if (data.resetUrl) setResetUrl(data.resetUrl);
    } catch (requestError) {
      setError(requestError.response?.data?.message || requestError.message || "Request failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell
      description="Enter your account email and we will send password reset instructions."
      eyebrow="Account recovery"
      footer={<p>Remembered your password? <Link to="/login">Back to sign in</Link></p>}
      title="Reset your password"
    >
      {error && <div className="error-message" role="alert">{error}</div>}
      {message && <div className="success-message" role="status">{message}</div>}
      {resetUrl && (
        <div className="reset-link-box">
          <strong>Development reset link</strong>
          <a href={resetUrl}>{resetUrl}</a>
        </div>
      )}
      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="auth-field">
          <span>Email address</span>
          <span className="auth-input-wrap">
            <Icon name="mail" size={18} />
            <input
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@company.com"
              required
              type="email"
              value={email}
            />
          </span>
        </label>
        <button className="btn btn-primary auth-submit" disabled={loading} type="submit">
          {loading ? "Sending…" : "Send reset link"}
          {!loading && <Icon name="arrowRight" size={17} />}
        </button>
      </form>
    </AuthShell>
  );
}
