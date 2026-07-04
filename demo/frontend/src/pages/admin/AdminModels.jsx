import { useEffect, useState } from "react";
import api from "../../api/axiosConfig";
import AdminLayout from "./AdminLayout";

export default function AdminModels() {
  const [models, setModels] = useState([]);

  const load = () => {
    api.get("/admin/ai-models").then((res) => setModels(res.data));
  };

  useEffect(() => {
    load();
  }, []);

  const toggle = async (id, active) => {
    await api.patch(`/admin/ai-models/${id}`, { active });
    load();
  };

  return (
    <AdminLayout title="AI models" subtitle="Enable or disable models available to users.">
      <div className="admin-card admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Provider</th>
              <th>Key</th>
              <th>Credits/use</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {models.map((m) => (
              <tr key={m.id}>
                <td>{m.displayName}</td>
                <td>{m.provider}</td>
                <td style={{ fontFamily: "monospace", fontSize: "0.75rem" }}>{m.modelKey}</td>
                <td>{m.creditsPerUse}</td>
                <td>{m.active ? "Active" : "Disabled"}</td>
                <td>
                  <button
                    type="button"
                    className={`admin-btn ${m.active ? "admin-btn-danger" : "admin-btn-primary"}`}
                    onClick={() => toggle(m.id, !m.active)}
                  >
                    {m.active ? "Disable" : "Enable"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  );
}
