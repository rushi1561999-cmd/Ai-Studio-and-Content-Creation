import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import api from "../api/axiosConfig";
import { setAuthSession } from "../utils/auth";

const WorkspaceContext = createContext(null);

export function WorkspaceProvider({ children }) {
  const [workspaceId, setWorkspaceId] = useState(null);
  const [workspaces, setWorkspaces] = useState([]);
  const [credits, setCredits] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refreshWallet = useCallback(async (wsId) => {
    const id = wsId || workspaceId;
    if (!id) return;
    const { data } = await api.get(`/ai/wallet/${id}`);
    setCredits(data.credits);
  }, [workspaceId]);

  const refreshUnread = useCallback(async () => {
    try {
      const { data } = await api.get("/notifications/unread-count");
      setUnreadCount(data.count ?? 0);
    } catch {
      setUnreadCount(0);
    }
  }, []);

  const switchWorkspace = useCallback(
    async (id) => {
      setWorkspaceId(id);
      localStorage.setItem("active_workspace_id", id);
      await refreshWallet(id);
    },
    [refreshWallet],
  );

  useEffect(() => {
    const init = async () => {
      setLoading(true);
      setError("");
      try {
        const meRes = await api.get("/auth/me");
        setAuthSession({
          token: localStorage.getItem("jwt_token"),
          role: meRes.data.role,
          email: meRes.data.email,
          fullName: meRes.data.fullName,
        });

        const { data: wsList } = await api.get("/workspaces");
        setWorkspaces(wsList);

        const saved = localStorage.getItem("active_workspace_id");
        const active = wsList.find((w) => w.id === saved) || wsList[0];
        if (active) {
          setWorkspaceId(active.id);
          localStorage.setItem("active_workspace_id", active.id);
          await Promise.all([refreshWallet(active.id), refreshUnread()]);
        } else if (meRes.data.role !== "ADMIN") {
          setError("No workspace found. Create one from settings.");
        }
      } catch (err) {
        setError(err.message || "Failed to load workspace.");
      } finally {
        setLoading(false);
      }
    };
    init();
  }, [refreshWallet, refreshUnread]);

  const value = {
    workspaceId,
    workspaces,
    credits,
    unreadCount,
    loading,
    error,
    refreshWallet,
    refreshUnread,
    switchWorkspace,
    setCredits,
  };

  return (
    <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>
  );
}

export function useWorkspace() {
  const ctx = useContext(WorkspaceContext);
  if (!ctx) {
    throw new Error("useWorkspace must be used within WorkspaceProvider");
  }
  return ctx;
}
