import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosConfig";
import AppLayout from "../components/AppLayout";
import { clearAuthSession, setAuthSession } from "../utils/auth";
import "./Settings.css";

export default function Settings() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState({ email: "", fullName: "", role: "" });
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [deletePassword, setDeletePassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    api
      .get("/auth/me")
      .then((res) => {
        setProfile(res.data);
        setFullName(res.data.fullName || "");
        setEmail(res.data.email || "");
      })
      .catch(() => setError("Could not load profile."));
  }, []);

  const handleUpdate = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    setMessage("");

    try {
      const body = { fullName, email };
      if (newPassword) {
        body.currentPassword = currentPassword;
        body.newPassword = newPassword;
      }

      const { data } = await api.put("/auth/me", body);
      setProfile(data.user);
      setMessage(data.message);

      if (data.token) {
        setAuthSession({
          token: data.token,
          role: data.user.role,
          email: data.user.email,
          fullName: data.user.fullName,
        });
      }

      setCurrentPassword("");
      setNewPassword("");
    } catch (err) {
      setError(err.response?.data?.message || "Update failed.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (e) => {
    e.preventDefault();
    if (!confirm("Delete your account permanently? This cannot be undone.")) {
      return;
    }

    setDeleting(true);
    setError("");

    try {
      await api.delete("/auth/me", { data: { password: deletePassword } });
      clearAuthSession();
      navigate("/login");
    } catch (err) {
      setError(err.response?.data?.message || "Could not delete account.");
      setDeleting(false);
    }
  };

  return (
    <AppLayout title="Account settings" subtitle="Update your profile or delete your account.">
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      <div className="settings-grid">
        <form className="card settings-form" onSubmit={handleUpdate}>
          <h3>Profile</h3>
          <p className="settings-meta">Role: {profile.role}</p>

          <label>
            Full name
            <input value={fullName} onChange={(e) => setFullName(e.target.value)} />
          </label>

          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>

          <h4>Change password</h4>
          <p className="settings-hint">Leave blank to keep your current password.</p>

          <label>
            Current password
            <input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
            />
          </label>

          <label>
            New password
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              minLength={6}
            />
          </label>

          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? "Saving…" : "Save changes"}
          </button>
        </form>

        <form className="card settings-danger" onSubmit={handleDelete}>
          <h3>Delete account</h3>
          <p>This removes your profile, workspace memberships, and related data.</p>

          <label>
            Confirm with password
            <input
              type="password"
              value={deletePassword}
              onChange={(e) => setDeletePassword(e.target.value)}
              required
            />
          </label>

          <button type="submit" className="btn-danger" disabled={deleting}>
            {deleting ? "Deleting…" : "Delete my account"}
          </button>
        </form>
      </div>
    </AppLayout>
  );
}
