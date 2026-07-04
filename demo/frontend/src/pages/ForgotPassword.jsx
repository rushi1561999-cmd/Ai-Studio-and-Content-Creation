import React, { useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/axiosConfig";
import "./Login.css";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [resetUrl, setResetUrl] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");
    setResetUrl("");

    try {
      const { data } = await api.post("/auth/forgot-password", { email });
      setMessage(data.message);
      if (data.resetUrl) {
        setResetUrl(data.resetUrl);
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Request failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>Forgot password</h2>
        <p>Enter your email and we&apos;ll send reset instructions.</p>

        {error && <div className="error-message">{error}</div>}
        {message && <div className="success-message">{message}</div>}

        {resetUrl && (
          <div className="reset-link-box">
            <p>Development reset link:</p>
            <a href={resetUrl}>{resetUrl}</a>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Email address</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <button type="submit" disabled={loading}>
            {loading ? "Sending…" : "Send reset link"}
          </button>
        </form>

        <p style={{ marginTop: "1.5rem", fontSize: "0.9rem" }}>
          <Link to="/login">Back to sign in</Link>
        </p>
      </div>
    </div>
  );
}
