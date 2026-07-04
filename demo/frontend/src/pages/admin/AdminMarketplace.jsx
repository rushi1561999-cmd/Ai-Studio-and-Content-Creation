import { useEffect, useState } from "react";
import api from "../../api/axiosConfig";
import AdminLayout from "./AdminLayout";

export default function AdminMarketplace() {
  const [posts, setPosts] = useState([]);

  const load = () => {
    api.get("/admin/marketplace/posts").then((res) => setPosts(res.data));
  };

  useEffect(() => {
    load();
  }, []);

  const remove = async (id) => {
    if (!confirm("Delete this marketplace post?")) return;
    try {
      await api.delete(`/admin/marketplace/posts/${id}`);
      load();
    } catch (err) {
      alert(err.response?.data?.message || "Delete failed.");
    }
  };

  return (
    <AdminLayout title="Marketplace moderation" subtitle="Review and remove community posts.">
      <div className="admin-card admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Author</th>
              <th>Category</th>
              <th>Prompt</th>
              <th>Likes</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {posts.map((p) => (
              <tr key={p.id}>
                <td>{p.authorName}</td>
                <td>{p.category}</td>
                <td style={{ maxWidth: 320 }}>{p.promptText?.substring(0, 80)}…</td>
                <td>{p.likes}</td>
                <td>
                  <button type="button" className="admin-btn admin-btn-danger" onClick={() => remove(p.id)}>
                    Delete
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
