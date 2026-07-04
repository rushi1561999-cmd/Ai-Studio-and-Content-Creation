
import React, { useEffect, useState } from "react";
import AppLayout from "../components/AppLayout";
import { useWorkspace } from "../context/WorkspaceContext";
import api from "../api/axiosConfig";
import "./Assets.css";

export default function Assets() {
  const { workspaceId, loading: wsLoading } = useWorkspace();
  const [assets, setAssets] = useState([]);
  const [folders, setFolders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [folderName, setFolderName] = useState("");
  const [assetForm, setAssetForm] = useState({
    name: "",
    mimeType: "text/plain",
    sizeBytes: 0,
  });
  const [message, setMessage] = useState("");

  const load = async () => {
    if (!workspaceId) return;
    setLoading(true);
    try {
      const [assetsRes, foldersRes] = await Promise.all([
        api.get(`/assets/workspace/${workspaceId}`),
        api.get(`/assets/workspace/${workspaceId}/folders`),
      ]);
      setAssets(assetsRes.data);
      setFolders(foldersRes.data);
    } catch (err) {
      setMessage(err.message || "Failed to load assets.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (workspaceId) load();
  }, [workspaceId]);

  const createFolder = async (e) => {
    e.preventDefault();
    if (!folderName.trim() || !workspaceId) return;
    try {
      await api.post(`/assets/workspace/${workspaceId}/folders`, {
        name: folderName.trim(),
        parentId: null,
      });
      setFolderName("");
      setMessage("Folder created.");
      await load();
    } catch (err) {
      setMessage(err.response?.data?.message || "Could not create folder.");
    }
  };

  const registerAsset = async (e) => {
    e.preventDefault();
    if (!workspaceId || !assetForm.name.trim()) return;
    try {
      await api.post("/assets", {
        workspaceId,
        folderId: folders[0]?.id || null,
        name: assetForm.name.trim(),
        mimeType: assetForm.mimeType,
        sizeBytes: Number(assetForm.sizeBytes) || 0,
      });
      setAssetForm({ name: "", mimeType: "text/plain", sizeBytes: 0 });
      setMessage("Asset registered.");
      await load();
    } catch (err) {
      setMessage(err.response?.data?.message || "Could not register asset.");
    }
  };

  return (
    <AppLayout
      title="Cloud Assets"
      subtitle="Manage folders and file metadata for your workspace."
    >
      {message && (
        <div
          className={`alert ${message.includes("Failed") ? "alert-error" : "alert-success"}`}
        >
          {message}
        </div>
      )}

      <div className="assets-layout">
        <section className="card assets-panel">
          <h3>Folders</h3>
          <form className="inline-form" onSubmit={createFolder}>
            <input
              value={folderName}
              onChange={(e) => setFolderName(e.target.value)}
              placeholder="New folder name"
            />
            <button type="submit" className="btn-primary">
              Add
            </button>
          </form>
          <ul className="folder-list">
            {folders.length === 0 ? (
              <li className="muted">No folders yet</li>
            ) : (
              folders.map((f) => <li key={f.id}>📁 {f.name}</li>)
            )}
          </ul>
        </section>

        <section className="card assets-panel assets-main">
          <h3>Register asset</h3>
          <form className="asset-form" onSubmit={registerAsset}>
            <input
              value={assetForm.name}
              onChange={(e) =>
                setAssetForm({ ...assetForm, name: e.target.value })
              }
              placeholder="File name"
              required
            />
            <input
              value={assetForm.mimeType}
              onChange={(e) =>
                setAssetForm({ ...assetForm, mimeType: e.target.value })
              }
              placeholder="MIME type"
            />
            <input
              type="number"
              value={assetForm.sizeBytes}
              onChange={(e) =>
                setAssetForm({ ...assetForm, sizeBytes: e.target.value })
              }
              placeholder="Size (bytes)"
              min={0}
            />
            <button type="submit" className="btn-primary">
              Register
            </button>
          </form>

          <h3 style={{ marginTop: "1.5rem" }}>Assets</h3>
          {wsLoading || loading ? (
            <p className="muted">Loading…</p>
          ) : assets.length === 0 ? (
            <p className="muted">No assets registered.</p>
          ) : (
            <table className="assets-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Size</th>
                  <th>Path</th>
                </tr>
              </thead>
              <tbody>
                {assets.map((a) => (
                  <tr key={a.id}>
                    <td>{a.name}</td>
                    <td>{a.mimeType || "—"}</td>
                    <td>{formatBytes(a.sizeBytes)}</td>
                    <td className="path-cell">{a.storagePath}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </AppLayout>
  );
}

function formatBytes(bytes) {
  if (!bytes) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
}
