import { useEffect, useState } from "react";
import api from "../../api/axiosConfig";
import AdminLayout from "./AdminLayout";

export default function AdminAuditLogs() {
  const [logs, setLogs] = useState([]);

  useEffect(() => {
    api.get("/admin/audit-logs").then((res) => setLogs(res.data));
  }, []);

  return (
    <AdminLayout title="Audit logs" subtitle="Recent platform activity (latest 200).">
      <div className="admin-card admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Action</th>
              <th>Entity</th>
              <th>Workspace</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id}>
                <td>{log.createdAt ? new Date(log.createdAt).toLocaleString() : "—"}</td>
                <td>{log.action}</td>
                <td>
                  {log.entityType} {log.entityId ? `(${log.entityId.substring(0, 8)}…)` : ""}
                </td>
                <td style={{ fontFamily: "monospace", fontSize: "0.75rem" }}>
                  {log.workspaceId ? log.workspaceId.substring(0, 8) + "…" : "—"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  );
}
