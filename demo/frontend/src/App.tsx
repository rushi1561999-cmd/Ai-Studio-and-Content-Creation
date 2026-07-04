import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Prompts from "./pages/Prompts";
import Marketplace from "./pages/Marketplace";
import Wallet from "./pages/Wallet";
import Assets from "./pages/Assets";
import Notifications from "./pages/Notifications";
import Settings from "./pages/Settings";
import Workspaces from "./pages/Workspaces";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";
import ProtectedRoute from "./components/ProtectedRoute";
import AdminRoute from "./components/AdminRoute";
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminUsers from "./pages/admin/AdminUsers";
import AdminWorkspaces from "./pages/admin/AdminWorkspaces";
import AdminMarketplace from "./pages/admin/AdminMarketplace";
import AdminAuditLogs from "./pages/admin/AdminAuditLogs";
import AdminPayments from "./pages/admin/AdminPayments";
import AdminModels from "./pages/admin/AdminModels";
import { isAdmin } from "./utils/auth";
import { ThemeProvider } from "./context/ThemeContext";

function HomeRedirect() {
  const token = localStorage.getItem("jwt_token");
  if (!token) return <Navigate to="/login" replace />;
  return <Navigate to={isAdmin() ? "/admin" : "/dashboard"} replace />;
}

function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomeRedirect />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/prompts"
            element={
              <ProtectedRoute>
                <Prompts />
              </ProtectedRoute>
            }
          />
          <Route
            path="/marketplace"
            element={
              <ProtectedRoute>
                <Marketplace />
              </ProtectedRoute>
            }
          />
          <Route
            path="/assets"
            element={
              <ProtectedRoute>
                <Assets />
              </ProtectedRoute>
            }
          />
          <Route
            path="/wallet"
            element={
              <ProtectedRoute>
                <Wallet />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <Notifications />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <Settings />
              </ProtectedRoute>
            }
          />
          <Route
            path="/workspaces"
            element={
              <ProtectedRoute>
                <Workspaces />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminDashboard />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <AdminRoute>
                <AdminUsers />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/workspaces"
            element={
              <AdminRoute>
                <AdminWorkspaces />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/marketplace"
            element={
              <AdminRoute>
                <AdminMarketplace />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/audit-logs"
            element={
              <AdminRoute>
                <AdminAuditLogs />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/payments"
            element={
              <AdminRoute>
                <AdminPayments />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/ai-models"
            element={
              <AdminRoute>
                <AdminModels />
              </AdminRoute>
            }
          />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;
