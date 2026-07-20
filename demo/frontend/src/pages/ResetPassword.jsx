import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import api from "../api/axiosConfig";
import AuthShell from "../components/AuthShell";
import Icon from "../components/Icon";
import "./Login.css";

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const tokenFromUrl = searchParams.get("token") || "";
  const [token, setToken] = useState(tokenFromUrl);
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (password !== confirm) return setError("Passwords do not match.");
    if (password.length < 6) return setError("Password must be at least 6 characters.");
    setLoading(true);
    setError("");
    setMessage("");
    try {
      const { data } = await api.post("/auth/reset-password", {
        token: token.trim(),
        newPassword: password,
      });
      setMessage(data.message);
      window.setTimeout(() => navigate("/login"), 2000);
    } catch (requestError) {
      setError(requestError.response?.data?.message || requestError.message || "Reset failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell
      description="Choose a strong new password for your AI Studio account."
      eyebrow="Secure your account"
      footer={<p><Link to="/login">Back to sign in</Link></p>}
      title="Choose a new password"
    >
      {error && <div className="error-message" role="alert">{error}</div>}
      {message && <div className="success-message" role="status">{message}</div>}
      <form className="auth-form" onSubmit={handleSubmit}>
        {!tokenFromUrl && (
          <label className="auth-field">
            <span>Reset token</span>
            <span className="auth-input-wrap">
              <Icon name="key" size={18} />
              <input
                onChange={(event) => setToken(event.target.value)}
                placeholder="Paste the token from your email"
                required
                value={token}
              />
            </span>
          </label>
        )}
        <label className="auth-field">
          <span>New password</span>
          <span className="auth-input-wrap">
            <Icon name="lock" size={18} />
            <input autoComplete="new-password" minLength={6} onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
          </span>
        </label>
        <label className="auth-field">
          <span>Confirm password</span>
          <span className="auth-input-wrap">
            <Icon name="lock" size={18} />
            <input autoComplete="new-password" minLength={6} onChange={(event) => setConfirm(event.target.value)} required type="password" value={confirm} />
          </span>
        </label>
        <button className="btn btn-primary auth-submit" disabled={loading} type="submit">
          {loading ? "Updating…" : "Update password"}
          {!loading && <Icon name="arrowRight" size={17} />}
        </button>
      </form>
    </AuthShell>
  );
}
