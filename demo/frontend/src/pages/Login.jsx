import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../api/axiosConfig";
import { setAuthSession, getHomePathForRole } from "../utils/auth";
import "./Login.css";

const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const response = await api.post("/auth/login", { email, password });
      setAuthSession(response.data);
      navigate(getHomePathForRole(response.data.role));
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.message ||
        "Invalid email or password. Please try again.";
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-background" />
      <div className="login-card card animate-fadeIn">
        <div className="login-header">
          <div className="brand-logo gradient-bg animate-gradient pulse-animation">AI</div>
          <h1 className="gradient-text">Welcome to AI Studio 🚀</h1>
          <p>Sign in to your enterprise workspace ✨</p>
        </div>

        {error && <div className="error-message badge badge-danger">⚠️ {error}</div>}

        <form onSubmit={handleLogin}>
          <div className="form-group">
            <label>📧 Email Address</label>
            <input
              type="email"
              className="input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="Enter your email"
            />
          </div>

          <div className="form-group">
            <label>🔒 Password</label>
            <input
              type="password"
              className="input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="Enter your password"
            />
          </div>

          <button type="submit" className="btn btn-primary" disabled={isLoading}>
            {isLoading ? "Signing In… ⏳" : "Sign In 🔐"}
          </button>
        </form>

        <div className="login-links">
          <Link to="/forgot-password" className="link">
            Forgot your password? 🔑
          </Link>
        </div>

        <div className="login-hint card">
          <p>Platform admin 👤:</p>
          <code>admin@aistudio.com</code>
          <code>Admin@123</code>
        </div>

        <div className="login-footer">
          <p>Don&apos;t have an account? 🤔</p>
          <Link to="/register" className="link">
            Sign up here 📝
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Login;
