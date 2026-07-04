import { Navigate } from "react-router-dom";
import { isAdmin } from "../utils/auth";

export default function AdminRoute({ children }) {
  const token = localStorage.getItem("jwt_token");

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin()) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}
