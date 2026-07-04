import { useEffect, useState } from "react";
import api from "../../api/axiosConfig";
import AdminLayout from "./AdminLayout";

export default function AdminWorkspaces() {
  const [workspaces, setWorkspaces] = useState([]);

  useEffect(() => {
    api.get("/admin/workspaces").then((res) => setWorkspaces(res.data));
  }, []);

  return (
    <AdminLayout title="Workspaces" subtitle="All workspaces on the platform.">
      <div className="admin-card admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {workspaces.map((w) => (
              <tr key={w.id}>
                <td style={{ fontFamily: "monospace", fontSize: "0.75rem" }}>{w.id}</td>
                <td>{w.name}</td>
                <td>{w.createdAt ? new Date(w.createdAt).toLocaleString() : "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  );
}
