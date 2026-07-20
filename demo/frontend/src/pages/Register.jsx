import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axiosConfig";
import AuthShell from "../components/AuthShell";
import Icon from "../components/Icon";
import { getHomePathForRole, setAuthSession } from "../utils/auth";
import "./Login.css";

export default function Register() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async (event) => {
    event.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const response = await api.post("/auth/register", {
        fullName: name,
        email,
        password,
      });
      setAuthSession(response.data);
      navigate(getHomePathForRole(response.data.role));
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          requestError.message ||
          "We could not create your account. That email may already be registered.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthShell
      description="Set up your account and start creating in minutes."
      eyebrow="Start creating"
      footer={<p>Already have an account? <Link to="/login">Sign in</Link></p>}
      title="Create your account"
    >
      {error && <div className="error-message" role="alert">{error}</div>}
      <form className="auth-form" onSubmit={handleRegister}>
        <label className="auth-field">
          <span>Full name</span>
          <span className="auth-input-wrap">
            <Icon name="user" size={18} />
            <input
              autoComplete="name"
              onChange={(event) => setName(event.target.value)}
              placeholder="Your full name"
              required
              type="text"
              value={name}
            />
          </span>
        </label>
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
        <label className="auth-field">
          <span>Password</span>
          <span className="auth-input-wrap">
            <Icon name="lock" size={18} />
            <input
              autoComplete="new-password"
              minLength={6}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="At least 6 characters"
              required
              type="password"
              value={password}
            />
          </span>
        </label>
        <button className="btn btn-primary auth-submit" disabled={isLoading} type="submit">
          {isLoading ? "Creating account…" : "Create account"}
          {!isLoading && <Icon name="arrowRight" size={17} />}
        </button>
      </form>
    </AuthShell>
  );
}
