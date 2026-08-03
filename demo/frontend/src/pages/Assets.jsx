import { useEffect, useState } from "react";
import AppLayout from "../components/AppLayout";
import { useWorkspace } from "../context/workspace-context";
import api from "../api/axiosConfig";
import "./Assets.css";

export default function Assets() {
  const { workspaceId, loading: workspaceLoading } = useWorkspace();
  const [assets, setAssets] = useState([]);
  const [folders, setFolders] = useState([]);
  const [folderName, setFolderName] = useState("");
  const [selectedFolder, setSelectedFolder] = useState("");
  const [selectedFile, setSelectedFile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");

  const reload = async () => {
    if (!workspaceId) return;
    const [assetsResponse, foldersResponse] = await Promise.all([
      api.get(`/assets/workspace/${workspaceId}`),
      api.get(`/assets/workspace/${workspaceId}/folders`),
    ]);
    setAssets(assetsResponse.data);
    setFolders(foldersResponse.data);
  };

  useEffect(() => {
    if (!workspaceId) return undefined;
    let cancelled = false;
    Promise.all([
      api.get(`/assets/workspace/${workspaceId}`),
      api.get(`/assets/workspace/${workspaceId}/folders`),
    ])
      .then(([assetsResponse, foldersResponse]) => {
        if (cancelled) return;
        setAssets(assetsResponse.data);
        setFolders(foldersResponse.data);
      })
      .catch((error) => {
        if (!cancelled) {
          setMessage(error.response?.data?.message || "Failed to load assets.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [workspaceId]);

  const createFolder = async (event) => {
    event.preventDefault();
    if (!folderName.trim() || !workspaceId) return;
    try {
      await api.post(`/assets/workspace/${workspaceId}/folders`, {
        name: folderName.trim(),
        parentId: null,
      });
      setFolderName("");
      setMessage("Folder created.");
      await reload();
    } catch (error) {
      setMessage(error.response?.data?.message || "Could not create folder.");
    }
  };

  const uploadAsset = async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (!workspaceId || !selectedFile) return;
    setUploading(true);
    setMessage("");
    const formData = new FormData();
    formData.append("file", selectedFile);
    try {
      const params = new URLSearchParams({ workspaceId });
      if (selectedFolder) params.set("folderId", selectedFolder);
      await api.post(`/assets?${params.toString()}`, formData);
      setSelectedFile(null);
      form.reset();
      setMessage("File uploaded and version 1 was recorded.");
      await reload();
    } catch (error) {
      setMessage(error.response?.data?.message || "File upload failed.");
    } finally {
      setUploading(false);
    }
  };

  const uploadVersion = async (asset, file) => {
    if (!file) return;
    const formData = new FormData();
    formData.append("file", file);
    try {
      await api.post(`/assets/${asset.id}/versions`, formData);
      setMessage(`A new version of ${asset.name} was uploaded.`);
      await reload();
    } catch (error) {
      setMessage(error.response?.data?.message || "Version upload failed.");
    }
  };

  const downloadAsset = async (asset) => {
    try {
      const response = await api.get(`/assets/${asset.id}/download`, {
        responseType: "blob",
      });
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement("a");
      link.href = url;
      link.download = asset.name;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      setMessage(error.response?.data?.message || "Download failed.");
    }
  };

  const deleteAsset = async (asset) => {
    if (!window.confirm(`Delete ${asset.name} and every stored version?`)) return;
    try {
      await api.delete(`/assets/${asset.id}`);
      setMessage(`${asset.name} was deleted.`);
      await reload();
    } catch (error) {
      setMessage(error.response?.data?.message || "Delete failed.");
    }
  };

  return (
    <AppLayout
      title="Asset library"
      subtitle="Upload, download, version, and securely remove real workspace files."
    >
      {message && (
        <div className={`alert ${/failed|could not/i.test(message) ? "alert-error" : "alert-success"}`}>
          {message}
        </div>
      )}

      <div className="assets-layout">
        <section className="card assets-panel">
          <h3>Folders</h3>
          <form className="inline-form" onSubmit={createFolder}>
            <input
              value={folderName}
              onChange={(event) => setFolderName(event.target.value)}
              placeholder="New folder name"
              maxLength={120}
              required
            />
            <button type="submit" className="btn-primary">Add</button>
          </form>
          <ul className="folder-list">
            {folders.length === 0 ? (
              <li className="muted">No folders yet</li>
            ) : (
              folders.map((folder) => <li key={folder.id}>{folder.name}</li>)
            )}
          </ul>
        </section>

        <section className="card assets-panel assets-main">
          <h3>Upload a file</h3>
          <form className="asset-form" onSubmit={uploadAsset}>
            <input
              type="file"
              onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
              required
            />
            <select
              value={selectedFolder}
              onChange={(event) => setSelectedFolder(event.target.value)}
            >
              <option value="">Workspace root</option>
              {folders.map((folder) => (
                <option key={folder.id} value={folder.id}>{folder.name}</option>
              ))}
            </select>
            <button type="submit" className="btn-primary" disabled={uploading}>
              {uploading ? "Uploading..." : "Upload"}
            </button>
          </form>

          <h3>Stored assets</h3>
          {workspaceLoading || loading ? (
            <p className="muted">Loading…</p>
          ) : assets.length === 0 ? (
            <p className="muted">No files uploaded.</p>
          ) : (
            <table className="assets-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Size</th>
                  <th>Uploaded</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {assets.map((asset) => (
                  <tr key={asset.id}>
                    <td>{asset.name}</td>
                    <td>{asset.mimeType || "—"}</td>
                    <td>{formatBytes(asset.sizeBytes)}</td>
                    <td>{formatDate(asset.createdAt)}</td>
                    <td>
                      <div className="asset-actions">
                        <button className="btn-small btn-secondary" onClick={() => downloadAsset(asset)} type="button">
                          Download
                        </button>
                        <label className="btn-small btn-secondary version-upload">
                          New version
                          <input
                            type="file"
                            onChange={(event) => uploadVersion(asset, event.target.files?.[0])}
                          />
                        </label>
                        <button className="btn-small btn-danger" onClick={() => deleteAsset(asset)} type="button">
                          Delete
                        </button>
                      </div>
                    </td>
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
  const units = ["B", "KB", "MB", "GB"];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : "—";
}
