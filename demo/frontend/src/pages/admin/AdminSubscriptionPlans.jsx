import { useEffect, useState } from "react";
import api from "../../api/axiosConfig";
import AdminLayout from "./AdminLayout";
import "./AdminSubscriptionPlans.css";

export default function AdminSubscriptionPlans() {
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [editingPlan, setEditingPlan] = useState(null);
  const [formData, setFormData] = useState({
    code: "",
    name: "",
    monthlyCredits: "",
    price: "",
    currency: "USD",
    active: true,
  });

  useEffect(() => {
    loadPlans();
  }, []);

  const loadPlans = async () => {
    try {
      setLoading(true);
      const res = await api.get("/admin/subscription-plans");
      setPlans(res.data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load subscription plans.");
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingPlan(null);
    setFormData({
      code: "",
      name: "",
      monthlyCredits: "",
      price: "",
      currency: "USD",
      active: true,
    });
    setShowModal(true);
  };

  const handleEdit = (plan) => {
    setEditingPlan(plan);
    setFormData({
      code: plan.code,
      name: plan.name,
      monthlyCredits: plan.monthlyCredits,
      price: plan.priceCents,
      currency: plan.currency || "USD",
      active: plan.active,
    });
    setShowModal(true);
  };

  const handleDelete = async (planId) => {
    if (!window.confirm("Are you sure you want to delete this subscription plan?")) {
      return;
    }

    try {
      await api.delete(`/admin/subscription-plans/${planId}`);
      await loadPlans();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to delete subscription plan.");
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const payload = {
        code: formData.code,
        name: formData.name,
        monthlyCredits: parseInt(formData.monthlyCredits),
        priceCents: parseInt(formData.price),
        currency: formData.currency,
        active: formData.active,
      };

      if (editingPlan) {
        await api.put(`/admin/subscription-plans/${editingPlan.id}`, payload);
      } else {
        await api.post("/admin/subscription-plans", payload);
      }

      setShowModal(false);
      await loadPlans();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to save subscription plan.");
    }
  };

  const formatPrice = (cents, currency) => {
    return cents.toString();
  };

  return (
    <AdminLayout
      title="Subscription Plans"
      subtitle="Manage subscription plans."
    >
      {error && (
        <div className="admin-error">
          ⚠️ {error}
        </div>
      )}

      <div className="admin-actions">
        <button className="btn btn-primary" onClick={handleCreate}>
          + Create Plan
        </button>
      </div>

      {loading ? (
        <div className="admin-loading">Loading...</div>
      ) : (
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Monthly Credits</th>
                <th>Price</th>
                <th>Currency</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {plans.map((plan) => (
                <tr key={plan.id}>
                  <td>{plan.code}</td>
                  <td>{plan.name}</td>
                  <td>{plan.monthlyCredits}</td>
                  <td>{formatPrice(plan.priceCents, plan.currency)}</td>
                  <td>
                    <span className={`currency-badge ${plan.currency}`}>
                      {plan.currency}
                    </span>
                  </td>
                  <td>
                    <span className={`status-badge ${plan.active ? "active" : "inactive"}`}>
                      {plan.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn-small btn-secondary"
                      onClick={() => handleEdit(plan)}
                    >
                      Edit
                    </button>
                    <button
                      className="btn-small btn-danger"
                      onClick={() => handleDelete(plan.id)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {plans.length === 0 && (
            <div className="admin-empty">
              No subscription plans found. Create your first plan to get started.
            </div>
          )}
        </div>
      )}

      {showModal && (
        <div className="admin-modal-overlay" onClick={() => setShowModal(false)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h3>{editingPlan ? "Edit Subscription Plan" : "Create Subscription Plan"}</h3>
              <button
                className="admin-modal-close"
                onClick={() => setShowModal(false)}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmit} className="admin-modal-form">
              <div className="form-group">
                <label>Plan Code *</label>
                <input
                  type="text"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  required
                  placeholder="e.g., BASIC, PRO, ENTERPRISE"
                />
              </div>

              <div className="form-group">
                <label>Plan Name *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  placeholder="e.g., Basic Plan"
                />
              </div>

              <div className="form-group">
                <label>Monthly Credits *</label>
                <input
                  type="number"
                  value={formData.monthlyCredits}
                  onChange={(e) => setFormData({ ...formData, monthlyCredits: e.target.value })}
                  required
                  min="1"
                  placeholder="e.g., 100"
                />
              </div>

              <div className="form-group">
                <label>Price *</label>
                <input
                  type="number"
                  value={formData.price}
                  onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                  required
                  min="0"
                  placeholder="e.g., 9 or 900"
                />
              </div>

              <div className="form-group">
                <label>Currency *</label>
                <select
                  value={formData.currency}
                  onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
                  required
                >
                  <option value="USD">USD ($)</option>
                  <option value="INR">INR (₹)</option>
                </select>
              </div>

              <div className="form-group checkbox-group">
                <label>
                  <input
                    type="checkbox"
                    checked={formData.active}
                    onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                  />
                  <span>Active</span>
                </label>
              </div>

              <div className="admin-modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  {editingPlan ? "Update Plan" : "Create Plan"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
