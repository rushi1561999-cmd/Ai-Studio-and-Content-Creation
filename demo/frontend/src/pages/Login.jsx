import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/axiosConfig";
import AuthShell from "../components/AuthShell";
import Icon from "../components/Icon";
import { getHomePathForRole, setAuthSession } from "../utils/auth";
import "./Login.css";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (event) => {
    event.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const response = await api.post("/auth/login", { email, password });
      setAuthSession(response.data);
      navigate(getHomePathForRole(response.data.role));
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          requestError.message ||
          "We could not sign you in. Check your email and password.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthShell
      description="Enter your details to continue to your workspace."
      eyebrow="Welcome back"
      footer={<p>New to AI Studio? <Link to="/register">Create an account</Link></p>}
      title="Sign in to continue"
    >
      {error && <div className="error-message" role="alert">{error}</div>}
      <form className="auth-form" onSubmit={handleLogin}>
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
          <span className="auth-label-row">
            <span>Password</span>
            <Link to="/forgot-password">Forgot password?</Link>
          </span>
          <span className="auth-input-wrap">
            <Icon name="lock" size={18} />
            <input
              autoComplete="current-password"
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter your password"
              required
              type="password"
              value={password}
            />
          </span>
        </label>

        <button className="btn btn-primary auth-submit" disabled={isLoading} type="submit">
          {isLoading ? "Signing in…" : "Sign in"}
          {!isLoading && <Icon name="arrowRight" size={17} />}
        </button>
      </form>
    </AuthShell>
  );
}
