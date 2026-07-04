import { useEffect, useState } from "react";
import api from "../../api/axiosConfig";
import AdminLayout from "./AdminLayout";

export default function AdminPayments() {
  const [payments, setPayments] = useState([]);

  useEffect(() => {
    api.get("/admin/payments").then((res) => setPayments(res.data));
  }, []);

  return (
    <AdminLayout title="Payments" subtitle="All completed and pending payment records.">
      <div className="admin-card admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Workspace</th>
              <th>Provider</th>
              <th>Amount</th>
              <th>Credits</th>
              <th>Status</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {payments.map((p) => (
              <tr key={p.id}>
                <td style={{ fontFamily: "monospace", fontSize: "0.75rem" }}>
                  {p.workspaceId?.substring(0, 8)}…
                </td>
                <td>{p.provider}</td>
                <td>${(p.amountCents / 100).toFixed(2)}</td>
                <td>{p.creditsGranted}</td>
                <td>{p.status}</td>
                <td>{p.createdAt ? new Date(p.createdAt).toLocaleString() : "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  );
}
