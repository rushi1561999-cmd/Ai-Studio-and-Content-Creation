import { useEffect, useState } from "react";
import api from "../../api/axiosConfig";
import AdminLayout from "./AdminLayout";

export default function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);
  const [creditAmount, setCreditAmount] = useState("");
  const [creditDescription, setCreditDescription] = useState("");
  const [addingCredits, setAddingCredits] = useState(false);

  const load = () => {
    setLoading(true);
    api
      .get("/admin/users")
      .then((res) => setUsers(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let cancelled = false;
    api.get("/admin/users")
      .then((res) => {
        if (!cancelled) setUsers(res.data);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const changeRole = async (userId, role) => {
    try {
      await api.patch(`/admin/users/${userId}/role`, { role });
      load();
    } catch (err) {
      alert(err.response?.data?.message || "Could not update role.");
    }
  };

  const deleteUser = async (userId, email) => {
    if (!confirm(`Delete user ${email}? This cannot be undone.`)) return;
    try {
      await api.delete(`/admin/users/${userId}`);
      load();
    } catch (err) {
      alert(err.response?.data?.message || "Could not delete user.");
    }
  };

  const addCredits = async (userId) => {
    if (!creditAmount || parseInt(creditAmount) <= 0) {
      alert("Please enter a valid credit amount.");
      return;
    }

    setAddingCredits(true);
    try {
      await api.post(`/admin/users/${userId}/add-credits`, {
        amount: parseInt(creditAmount),
        description: creditDescription || "Admin credit addition"
      });
      alert("Credits added successfully!");
      setCreditAmount("");
      setCreditDescription("");
      setSelectedUser(null);
    } catch (err) {
      alert(err.response?.data?.message || "Could not add credits.");
    } finally {
      setAddingCredits(false);
    }
  };

  return (
    <AdminLayout title="Users" subtitle="Manage platform accounts and admin access.">
      <div className="admin-card admin-table-wrap">
        {loading ? (
          <p style={{ color: "#94a3b8" }}>Loading…</p>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Email</th>
                <th>Name</th>
                <th>Role</th>
                <th>Joined</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td>{u.fullName || "—"}</td>
                  <td>
                    <span className={`role-badge ${u.role.toLowerCase()}`}>{u.role}</span>
                  </td>
                  <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "—"}</td>
                  <td>
                    <select
                      className="admin-select"
                      value={u.role}
                      onChange={(e) => changeRole(u.id, e.target.value)}
                    >
                      <option value="USER">USER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                    <button
                      type="button"
                      className="admin-btn"
                      style={{ marginLeft: "0.5rem" }}
                      onClick={() => setSelectedUser(u)}
                    >
                      Add Credits
                    </button>
                    <button
                      type="button"
                      className="admin-btn admin-btn-danger"
                      style={{ marginLeft: "0.5rem" }}
                      onClick={() => deleteUser(u.id, u.email)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {selectedUser && (
        <div className="modal-overlay" style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, backgroundColor: "rgba(0,0,0,0.5)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 }}>
          <div className="card" style={{ padding: "2rem", minWidth: "400px", maxWidth: "500px" }}>
            <h3 style={{ marginBottom: "1rem" }}>Add Credits to {selectedUser.email}</h3>
            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem" }}>Credit Amount:</label>
              <input
                type="number"
                value={creditAmount}
                onChange={(e) => setCreditAmount(e.target.value)}
                placeholder="Enter amount"
                style={{ width: "100%", padding: "0.5rem", border: "1px solid #ccc", borderRadius: "4px" }}
              />
            </div>
            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem" }}>Description (optional):</label>
              <input
                type="text"
                value={creditDescription}
                onChange={(e) => setCreditDescription(e.target.value)}
                placeholder="Reason for adding credits"
                style={{ width: "100%", padding: "0.5rem", border: "1px solid #ccc", borderRadius: "4px" }}
              />
            </div>
            <div style={{ display: "flex", gap: "1rem", justifyContent: "flex-end" }}>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setSelectedUser(null)}
                disabled={addingCredits}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn-primary"
                onClick={() => addCredits(selectedUser.id)}
                disabled={addingCredits}
              >
                {addingCredits ? "Adding..." : "Add Credits"}
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
