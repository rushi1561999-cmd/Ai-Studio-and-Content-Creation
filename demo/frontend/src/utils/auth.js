export function setAuthSession(data) {
  localStorage.setItem("jwt_token", data.token);
  localStorage.setItem("user_role", data.role || "USER");
  localStorage.setItem("user_email", data.email || "");
  localStorage.setItem("user_name", data.fullName || "");
}

export function clearAuthSession() {
  localStorage.removeItem("jwt_token");
  localStorage.removeItem("user_role");
  localStorage.removeItem("user_email");
  localStorage.removeItem("user_name");
}

export function isAdmin() {
  return localStorage.getItem("user_role") === "ADMIN";
}

export function getRole() {
  return localStorage.getItem("user_role") || "USER";
}

export function getHomePathForRole(role) {
  return role === "ADMIN" ? "/admin" : "/dashboard";
}
