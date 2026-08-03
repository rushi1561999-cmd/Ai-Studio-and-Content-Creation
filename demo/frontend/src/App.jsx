import { lazy, Suspense } from "react";
import {
  BrowserRouter,
  Navigate,
  Outlet,
  Route,
  Routes,
} from "react-router";
import ProtectedRoute from "./components/ProtectedRoute";
import AdminRoute from "./components/AdminRoute";
import { WorkspaceProvider } from "./context/WorkspaceContext";
import { ThemeProvider } from "./context/ThemeContext";
import { isAdmin } from "./utils/auth";

const Login = lazy(() => import("./pages/Login"));
const Register = lazy(() => import("./pages/Register"));
const ForgotPassword = lazy(() => import("./pages/ForgotPassword"));
const ResetPassword = lazy(() => import("./pages/ResetPassword"));
const Dashboard = lazy(() => import("./pages/Dashboard"));
const Prompts = lazy(() => import("./pages/Prompts"));
const Marketplace = lazy(() => import("./pages/Marketplace"));
const Wallet = lazy(() => import("./pages/Wallet"));
const Assets = lazy(() => import("./pages/Assets"));
const Notifications = lazy(() => import("./pages/Notifications"));
const Settings = lazy(() => import("./pages/Settings"));
const Workspaces = lazy(() => import("./pages/Workspaces"));
const AdminDashboard = lazy(() => import("./pages/admin/AdminDashboard"));
const AdminUsers = lazy(() => import("./pages/admin/AdminUsers"));
const AdminWorkspaces = lazy(() => import("./pages/admin/AdminWorkspaces"));
const AdminMarketplace = lazy(() => import("./pages/admin/AdminMarketplace"));
const AdminAuditLogs = lazy(() => import("./pages/admin/AdminAuditLogs"));
const AdminPayments = lazy(() => import("./pages/admin/AdminPayments"));
const AdminSubscriptionPlans = lazy(
  () => import("./pages/admin/AdminSubscriptionPlans"),
);
const AdminModels = lazy(() => import("./pages/admin/AdminModels"));

function HomeRedirect() {
  const token = localStorage.getItem("jwt_token");
  if (!token) return <Navigate to="/login" replace />;
  return <Navigate to={isAdmin() ? "/admin" : "/dashboard"} replace />;
}

function WorkspaceRoutes() {
  return (
    <ProtectedRoute>
      <WorkspaceProvider>
        <Outlet />
      </WorkspaceProvider>
    </ProtectedRoute>
  );
}

function AdminRoutes() {
  return (
    <AdminRoute>
      <Outlet />
    </AdminRoute>
  );
}

function RouteFallback() {
  return (
    <div className="route-loading" role="status">
      <span className="spinner" />
      <span>Loading workspaceâ€¦</span>
    </div>
  );
}

function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <Suspense fallback={<RouteFallback />}>
          <Routes>
            <Route path="/" element={<HomeRedirect />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />

            <Route element={<WorkspaceRoutes />}>
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/prompts" element={<Prompts />} />
              <Route path="/marketplace" element={<Marketplace />} />
              <Route path="/assets" element={<Assets />} />
              <Route path="/wallet" element={<Wallet />} />
              <Route path="/notifications" element={<Notifications />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="/workspaces" element={<Workspaces />} />
            </Route>

            <Route element={<AdminRoutes />}>
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/admin/users" element={<AdminUsers />} />
              <Route path="/admin/workspaces" element={<AdminWorkspaces />} />
              <Route path="/admin/marketplace" element={<AdminMarketplace />} />
              <Route path="/admin/audit-logs" element={<AdminAuditLogs />} />
              <Route path="/admin/payments" element={<AdminPayments />} />
              <Route
                path="/admin/subscription-plans"
                element={<AdminSubscriptionPlans />}
              />
              <Route path="/admin/ai-models" element={<AdminModels />} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;
