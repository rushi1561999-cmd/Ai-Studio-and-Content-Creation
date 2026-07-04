import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../api/axiosConfig";
import { setAuthSession, getHomePathForRole } from "../utils/auth";
import "./Login.css";

const Register = () => {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
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
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.message ||
        "Registration failed. That email might already be in use.";
      setError(message);
      console.error("Registration error:", err);
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
          <h1 className="gradient-text">Create an Account 🎉</h1>
          <p>Join AI Studio to start generating content ✨</p>
        </div>

        {error && <div className="error-message badge badge-danger">⚠️ {error}</div>}

        <form onSubmit={handleRegister}>
          <div className="form-group">
            <label>👤 Full Name</label>
            <input
              type="text"
              className="input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              placeholder="Enter your full name"
            />
          </div>

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
              placeholder="Create a password"
            />
          </div>

          <button type="submit" className="btn btn-primary" disabled={isLoading}>
            {isLoading ? "Creating Account… ⏳" : "Sign Up 🚀"}
          </button>
        </form>

        <div className="login-footer">
          <p>Already have an account? 🤔</p>
          <Link to="/login" className="link">
            Log in here 🔐
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Register;
