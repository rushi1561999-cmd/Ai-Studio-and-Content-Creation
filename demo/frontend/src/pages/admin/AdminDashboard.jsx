import { useEffect, useState } from "react";
import api from "../../api/axiosConfig";
import AdminLayout from "./AdminLayout";

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .get("/admin/stats")
      .then((res) => setStats(res.data))
      .catch((err) => setError(err.response?.data?.message || "Failed to load stats."));
  }, []);

  return (
    <AdminLayout title="Platform overview" subtitle="Real-time counts across the system.">
      {error && <p style={{ color: "#fca5a5" }}>{error}</p>}
      {stats && (
        <div className="admin-stats-grid">
          <div className="admin-stat">
            <label>Users</label>
            <strong>{stats.totalUsers}</strong>
          </div>
          <div className="admin-stat">
            <label>Workspaces</label>
            <strong>{stats.totalWorkspaces}</strong>
          </div>
          <div className="admin-stat">
            <label>Marketplace posts</label>
            <strong>{stats.totalMarketplacePosts}</strong>
          </div>
          <div className="admin-stat">
            <label>Generation jobs</label>
            <strong>{stats.totalGenerationJobs}</strong>
          </div>
          <div className="admin-stat">
            <label>Payments</label>
            <strong>{stats.totalPayments}</strong>
          </div>
          <div className="admin-stat">
            <label>Credits in wallets</label>
            <strong>{stats.totalCreditsInWallets}</strong>
          </div>
        </div>
      )}
      <div className="admin-card">
        <h3 style={{ margin: "0 0 0.5rem", color: "#fafafa" }}>Quick actions</h3>
        <p style={{ margin: 0, color: "#94a3b8", fontSize: "0.9rem" }}>
          Manage users, moderate marketplace content, review audit logs, and toggle AI models from
          the sidebar.
        </p>
      </div>
    </AdminLayout>
  );
}
