import axios from "axios";
import { clearAuthSession } from "../utils/auth";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
});

function isAuthRoute(url = "") {
  return (
    url.includes("/auth/login") ||
    url.includes("/auth/register") ||
    url.includes("/auth/forgot-password") ||
    url.includes("/auth/reset-password")
  );
}

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("jwt_token");
    if (token && !isAuthRoute(config.url)) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      error.message =
        "Cannot reach the server. Is the backend running on port 8081?";
      return Promise.reject(error);
    }

    if (error.response.status === 401 && !isAuthRoute(error.config?.url)) {
      clearAuthSession();
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    }

    if (error.response.status === 403 && error.config?.url?.includes("/admin")) {
      error.message = "Admin access required.";
    }
    return Promise.reject(error);
  },
);

export async function resolveWorkspaceId() {
  const saved = localStorage.getItem("active_workspace_id");
  const { data: workspaces } = await api.get("/workspaces");
  if (workspaces.length > 0) {
    const match = workspaces.find((w) => w.id === saved);
    return match ? match.id : workspaces[0].id;
  }
  const { data: created } = await api.post("/workspaces", { name: "My Workspace" });
  localStorage.setItem("active_workspace_id", created.id);
  return created.id;
}

export default api;
