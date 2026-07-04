import React, { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import api from "../api/axiosConfig";
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    if (password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const { data } = await api.post("/auth/reset-password", {
        token: token.trim(),
        newPassword: password,
      });
      setMessage(data.message);
      setTimeout(() => navigate("/login"), 2000);
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Reset failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>Reset password</h2>
        <p>Choose a new password for your account.</p>

        {error && <div className="error-message">{error}</div>}
        {message && <div className="success-message">{message}</div>}

        <form onSubmit={handleSubmit}>
          {!tokenFromUrl && (
            <div className="form-group">
              <label>Reset token</label>
              <input
                value={token}
                onChange={(e) => setToken(e.target.value)}
                required
                placeholder="Paste token from email"
              />
            </div>
          )}
          <div className="form-group">
            <label>New password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
            />
          </div>
          <div className="form-group">
            <label>Confirm password</label>
            <input
              type="password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              required
              minLength={6}
            />
          </div>
          <button type="submit" disabled={loading}>
            {loading ? "Updating…" : "Update password"}
          </button>
        </form>

        <p style={{ marginTop: "1.5rem", fontSize: "0.9rem" }}>
          <Link to="/login">Back to sign in</Link>
        </p>
      </div>
    </div>
  );
}
