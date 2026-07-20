import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../api/axiosConfig";
import AppLayout from "../components/AppLayout";
import Icon from "../components/Icon";
import { useWorkspace } from "../context/workspace-context";
import "./Wallet.css";
import UpiQrModal from "../components/UpiQrModal";

const PACKS = {
  Starter: {
    pack: "starter",
    credits: 100,
    price: "90",
    gradient: "var(--primary-gradient)",
    emoji: "S",
  },
  Professional: {
    pack: "professional",
    credits: 500,
    price: "299",
    gradient: "var(--accent-gradient)",
    emoji: "P",
  },
  Enterprise: {
    pack: "enterprise",
    credits: 1000,
    price: "799",
    gradient: "var(--success-gradient)",
    emoji: "E",
  },
};

export default function Wallet() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { workspaceId, credits, refreshWallet } = useWorkspace();

  const [purchasing, setPurchasing] = useState(false);
  const [stripeEnabled, setStripeEnabled] = useState(false);
  const [razorpayEnabled, setRazorpayEnabled] = useState(false);
  const [upiEnabled, setUpiEnabled] = useState(false);
  const [paymentNotice, setPaymentNotice] = useState("");
  const [showUpiQr, setShowUpiQr] = useState(false);
  const [upiQrData, setUpiQrData] = useState(null);
  const [plans, setPlans] = useState([]);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [subscribing, setSubscribing] = useState(false);
  const [transactions, setTransactions] = useState([]);
  const [payments, setPayments] = useState([]);
  const [paymentMethods, setPaymentMethods] = useState([]);
  const [subscription, setSubscription] = useState(null);
  const [showPaymentForm, setShowPaymentForm] = useState(false);
  const [selectedPack, setSelectedPack] = useState(null);

  useEffect(() => {
    const load = async () => {
      try {
        const statusRes = await api.get("/stripe/status");
        setStripeEnabled(statusRes.data.enabled);
        const razorRes = await api.get("/razorpay/status");
        setRazorpayEnabled(razorRes.data.enabled);
        setUpiEnabled(razorRes.data.upiEnabled || false);
        const plansRes = await api.get("/billing/plans");
        setPlans(plansRes.data);
      } catch (err) {
        console.error(err);
      }
    };
    load();
  }, []);

  useEffect(() => {
    if (!workspaceId) return;
    const loadBilling = async () => {
      try {
        const [txRes, payRes, methodsRes, subRes] = await Promise.all([
          api.get(`/billing/workspace/${workspaceId}/transactions`),
          api.get(`/billing/workspace/${workspaceId}/payments`),
          api
            .get(`/billing/workspace/${workspaceId}/payment-methods`)
            .catch(() => ({ data: [] })),
          api
            .get(`/billing/workspace/${workspaceId}/subscription`)
            .catch(() => ({ data: null })),
        ]);
        setTransactions(txRes.data);
        setPayments(payRes.data);
        setPaymentMethods(methodsRes.data);
        setSubscription(subRes.data);
      } catch (err) {
        console.error(err);
      }
    };
    loadBilling();
    refreshWallet();
  }, [workspaceId, refreshWallet]);

  useEffect(() => {
    const payment = searchParams.get("payment");
    if (!payment) return undefined;
    const timer = window.setTimeout(() => {
      if (payment === "success") {
        setPaymentNotice("Payment successful! Credits have been added.");
        refreshWallet();
      } else if (payment === "cancelled") {
        setPaymentNotice("Payment cancelled.");
      }
      setSearchParams({});
    }, 0);
    return () => window.clearTimeout(timer);
  }, [searchParams, setSearchParams, refreshWallet]);

  const handlePurchase = async (tierName) => {
    const tier = PACKS[tierName];
    if (!workspaceId || !tier) return;

    setPurchasing(true);
    setPaymentNotice("");

    try {
      console.log(
        "handlePurchase called with tierName:",
        tierName,
        "tier:",
        tier,
      );
      console.log(
        "razorpayEnabled:",
        razorpayEnabled,
        "stripeEnabled:",
        stripeEnabled,
        "upiEnabled:",
        upiEnabled,
      );

      // Prioritize Razorpay over Stripe
      if (razorpayEnabled) {
        console.log("Attempting Razorpay checkout");
        await handleRazorpayCheckout(workspaceId, tier);
        setSelectedPack(null); // Clear selection after purchase
        return;
      }

      if (stripeEnabled) {
        console.log("Attempting Stripe checkout");
        const { data } = await api.post(
          `/stripe/checkout?workspaceId=${workspaceId}&pack=${tier.pack}`,
        );
        window.location.assign(data.checkoutUrl);
        setSelectedPack(null); // Clear selection after purchase
        return;
      }

      console.log("Using dev mode top-up");
      await api.post(`/ai/wallet/${workspaceId}/topup?amount=${tier.credits}`);
      await refreshWallet();
      setPaymentNotice(`Added ${tier.credits} credits (dev mode).`);
      setSelectedPack(null); // Clear selection after purchase
    } catch (error) {
      console.error("Purchase error:", error);
      alert(error.response?.data?.message || "Purchase failed.");
    } finally {
      setPurchasing(false);
    }
  };

  const handleUpiPayment = async (tierName) => {
    const tier = PACKS[tierName];
    if (!workspaceId || !tier) return;

    setPurchasing(true);
    setPaymentNotice("");

    try {
      console.log("Generating UPI QR for pack:", tier.pack);
      // Use subscription plan ID if available, otherwise fallback to pack name
      const planId = selectedPlan ? selectedPlan.id : tier.pack;
      const { data } = await api.post(
        `/razorpay/upi/qr?workspaceId=${workspaceId}&pack=${planId}`,
      );

      console.log("UPI QR generated:", data);
      setUpiQrData(data);
      setShowUpiQr(true);
      setPaymentNotice(
        "Scan the QR code with your UPI app to complete payment.",
      );
    } catch (error) {
      console.error("UPI QR generation error:", error);
      setPaymentNotice("Failed to generate a UPI QR. Please try again.");
    } finally {
      setPurchasing(false);
    }
  };

  const handleRazorpayCheckout = async (wsId, tier) => {
    try {
      console.log(
        "Starting Razorpay checkout for workspace:",
        wsId,
        "pack:",
        tier.pack,
      );

      // Check if Razorpay is loaded
      if (!window.Razorpay) {
        console.error("Razorpay SDK not loaded");
        setPaymentNotice(
          "Razorpay could not load. Please refresh the page.",
        );
        setPurchasing(false);
        return;
      }

      console.log("Razorpay SDK loaded, creating order...");

      // Get order details from backend
      const { data } = await api.post(
        `/razorpay/order?workspaceId=${wsId}&pack=${tier.pack}`,
      );

      console.log("Razorpay order created successfully:", data);

      const options = {
        key: data.key, // Razorpay Key ID from backend
        amount: data.amount, // Amount in paise
        currency: data.currency,
        order_id: data.orderId,
        name: "AI Studio",
        description: `${tier.credits} Credits Pack`,
        prefill: {
          email: data.email,
          name: data.name,
        },
        theme: {
          color: "#8b5cf6", // Purple color matching new theme
        },
        handler: async (response) => {
          console.log("Payment successful:", response);
          // Verify payment on backend
          try {
            await api.post("/razorpay/verify", {
              orderId: response.razorpay_order_id,
              paymentId: response.razorpay_payment_id,
              signature: response.razorpay_signature,
            });

            setPaymentNotice(
              `Payment successful! ${tier.credits} credits added.`,
            );
            await refreshWallet();
            setPurchasing(false);
          } catch (err) {
            setPaymentNotice(
              "Payment verification failed. Please contact support.",
            );
            console.error("Verification error:", err);
            setPurchasing(false);
          }
        },
        modal: {
          ondismiss: () => {
            console.log("Payment modal dismissed");
            setPaymentNotice("Payment cancelled.");
            setPurchasing(false);
          },
        },
      };

      console.log("Opening Razorpay modal...");
      const razorpay = new window.Razorpay(options);
      razorpay.open();
      console.log("Razorpay modal opened");
    } catch (error) {
      console.error("Razorpay checkout error:", error);
      setPaymentNotice(
        "Failed to initiate payment. Please try again. Error: " +
          (error.response?.data?.message || error.message),
      );
      setPurchasing(false);
    }
  };

  const handleSubscribePlan = async (planId) => {
    if (!workspaceId || !planId) return;

    setSubscribing(true);
    setPaymentNotice("");

    try {
      // Use Razorpay for subscriptions if enabled
      if (razorpayEnabled) {
        await api.post(`/billing/workspace/${workspaceId}/subscribe`, {
          planId,
        });
        const subRes = await api.get(
          `/billing/workspace/${workspaceId}/subscription`,
        );
        setSubscription(subRes.data);
        setSelectedPlan(null);
        setPaymentNotice("Subscription activated successfully.");
        return;
      }

      if (stripeEnabled) {
        const { data } = await api.post(
          `/stripe/subscribe?workspaceId=${workspaceId}&planId=${planId}`,
        );
        window.location.assign(data.checkoutUrl);
        return;
      }

      await api.post(`/billing/workspace/${workspaceId}/subscribe`, { planId });
      const subRes = await api.get(
        `/billing/workspace/${workspaceId}/subscription`,
      );
      setSubscription(subRes.data);
      setSelectedPlan(null);
      setPaymentNotice("Subscription activated successfully.");
    } catch (error) {
      setPaymentNotice(
        error.response?.data?.message || "Subscription failed.",
      );
    } finally {
      setSubscribing(false);
    }
  };

  const handleCancelSubscription = async () => {
    if (!workspaceId || !subscription?.id) return;

    if (!window.confirm("Cancel subscription? You'll lose recurring credits."))
      return;

    try {
      await api.post(
        `/billing/workspace/${workspaceId}/subscription/${subscription.id}/cancel`,
      );
      setSubscription(null);
      setPaymentNotice("Subscription cancelled.");
    } catch (error) {
      setPaymentNotice(
        error.response?.data?.message || "Cancellation failed.",
      );
    }
  };

  return (
    <AppLayout
      title="Billing and usage"
      subtitle={
        razorpayEnabled
          ? "Manage credits, plans, and secure payments with Razorpay."
          : stripeEnabled
            ? "Manage credits, plans, and secure payments with Stripe."
            : "Review credit usage and use development top-ups while payments are unconfigured."
      }
    >
      {paymentNotice && (
        <div
          className={`alert ${paymentNotice.includes("successful") || paymentNotice.includes("Added") ? "alert-success" : "alert-warning"}`}
        >
          {paymentNotice}
        </div>
      )}

      <div className="balance-banner card animate-fadeIn">
        <div className="balance-icon gradient-bg">
          <Icon name="bolt" size={24} />
        </div>
        <div className="balance-info">
          <h3>Available balance</h3>
          <h1
            className={
              credits <= 0 ? "zero-balance gradient-text" : "gradient-text"
            }
          >
            {credits} credits
          </h1>
          <p>
            {credits <= 0
              ? "You are out of credits. Choose a pack below to continue."
              : "Ready to use across supported AI generation models."}
          </p>
        </div>
      </div>

      {plans.length > 0 && (
        <section className="billing-section animate-fadeIn">
          <h2>Monthly plans</h2>
          {subscription ? (
            <div className="subscription-active card">
              <div className="subscription-content">
                <h4>Active subscription</h4>
                <p className="subscription-plan">{subscription.planName}</p>
                <p className="subscription-details">
                  {subscription.monthlyCredits} credits/month ·{" "}
                  {subscription.status}
                </p>
                <p className="subscription-date">
                  Renews: {formatDate(subscription.renewalDate)}
                </p>
              </div>
              <button
                className="btn btn-danger"
                onClick={handleCancelSubscription}
              >
                Cancel subscription
              </button>
            </div>
          ) : (
            <div className="plans-selection">
              {plans.map((plan) => (
                <div
                  key={plan.id}
                  className={`plan-card card ${selectedPlan?.id === plan.id ? "selected" : ""}`}
                  onClick={() => setSelectedPlan(plan)}
                >
                  <div className="plan-header">
                    <h4>{plan.name}</h4>
                    <span
                      className={`plan-status-badge ${selectedPlan?.id === plan.id ? "active" : ""}`}
                    >
                      {selectedPlan?.id === plan.id ? "✓ Selected" : "Select"}
                    </span>
                  </div>
                  <p className="plan-price">{plan.priceCents}/mo</p>
                  <p className="plan-credits">
                    {plan.monthlyCredits} credits monthly
                  </p>
                  <ul className="plan-features">
                    <li>Workspace storage</li>
                    <li>Marketplace access</li>
                    <li>AI generations</li>
                  </ul>
                </div>
              ))}
            </div>
          )}

          {selectedPlan && !subscription && (
            <div className="plan-subscribe-btn">
              <button
                className="btn btn-primary"
                disabled={subscribing || !workspaceId}
                onClick={() => handleSubscribePlan(selectedPlan.id)}
              >
                {subscribing
                  ? "Processing..."
                  : `Subscribe to ${selectedPlan.name}`}
              </button>
            </div>
          )}
        </section>
      )}

      <h2 className="section-title">One-time credit packs</h2>
      <div className="pricing-grid">
        {Object.entries(PACKS).map(([name, tier]) => (
          <div
            key={name}
            className={`pricing-card card hover-lift ${name === "Professional" ? "popular" : ""} ${selectedPack === name ? "selected" : ""}`}
            onClick={() => setSelectedPack(name)}
          >
            {name === "Professional" && (
              <div className="popular-badge badge badge-warning">
                Popular
              </div>
            )}
            <div
              className="pricing-header"
              style={{ background: tier.gradient }}
            >
              <div className="pack-emoji">{tier.emoji}</div>
              <h3>{name}</h3>
            </div>
            <h2>
              ₹{tier.price}
              <span>/pack</span>
            </h2>
            <ul>
              <li>{tier.credits} generation credits</li>
              <li>Marketplace access</li>
              <li>Workspace storage</li>
            </ul>
            <button
              type="button"
              className="btn btn-primary"
              disabled={purchasing || !workspaceId}
              onClick={(e) => {
                e.stopPropagation();
                handlePurchase(name);
              }}
            >
              {razorpayEnabled
                ? "Pay with Razorpay"
                : stripeEnabled
                  ? "Pay with Stripe"
                  : `Add ${tier.credits} credits`}
            </button>
            {upiEnabled && (
              <button
                type="button"
                className="btn btn-secondary"
                disabled={purchasing || !workspaceId}
                onClick={(e) => {
                  e.stopPropagation();
                  handleUpiPayment(name);
                }}
              >
                Pay with UPI
              </button>
            )}
          </div>
        ))}
      </div>

      {selectedPack && (
        <div className="plan-subscribe-btn">
          <button
            className="btn btn-primary"
            disabled={purchasing || !workspaceId}
            onClick={() => handlePurchase(selectedPack)}
          >
            {purchasing
              ? "Processing..."
              : `Purchase ${PACKS[selectedPack].credits} credits for ₹${PACKS[selectedPack].price}`}
          </button>
          <button
            className="btn btn-secondary"
            onClick={() => setSelectedPack(null)}
          >
            Cancel
          </button>
        </div>
      )}

      {(stripeEnabled || razorpayEnabled) && (
        <section className="card animate-fadeIn payment-methods-section">
          <h3>Payment methods</h3>
          {razorpayEnabled && !stripeEnabled ? (
            <div className="empty-state">
              <p>Razorpay is configured and ready for payments.</p>
              <p style={{ fontSize: "0.9rem", color: "var(--text-secondary)" }}>
                Click "Pay with Razorpay" to purchase credits instantly.
              </p>
            </div>
          ) : paymentMethods.length === 0 ? (
            <div className="empty-state">
              <p>No payment methods saved. Add one to speed up purchases.</p>
              <button
                className="btn btn-primary"
                onClick={() => setShowPaymentForm(!showPaymentForm)}
              >
                {showPaymentForm ? "Cancel ✕" : "Add payment method +"}
              </button>
              {showPaymentForm && (
                <div className="payment-form-placeholder">
                  <p>Stripe payment form will appear here.</p>
                  <p
                    style={{
                      fontSize: "0.85rem",
                      color: "var(--text-secondary)",
                    }}
                  >
                    (Backend integration required)
                  </p>
                </div>
              )}
            </div>
          ) : (
            <div className="payment-methods-list">
              {paymentMethods.map((method) => (
                <div key={method.id} className="payment-method-card">
                  <div className="method-info">
                    <span className="method-brand">
                      {method.brand?.toUpperCase()}
                    </span>
                    <span className="method-number">•••• {method.last4}</span>
                    <span className="method-expiry">
                      Exp: {method.expMonth}/{method.expYear}
                    </span>
                  </div>
                  <div className="method-actions">
                    {method.isDefault && (
                      <span className="badge badge-success">Default</span>
                    )}
                    <button className="btn-small btn-danger">Remove</button>
                  </div>
                </div>
              ))}
              <button
                className="btn btn-secondary"
                onClick={() => setShowPaymentForm(!showPaymentForm)}
              >
                + Add another method
              </button>
            </div>
          )}
        </section>
      )}

      <div className="billing-tables">
        <section className="card animate-fadeIn">
          <h3>Credit transactions</h3>
          {transactions.length === 0 ? (
            <div className="empty-state">
              <p>No transactions yet.</p>
            </div>
          ) : (
            <table className="billing-table">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Balance</th>
                  <th>Description</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => (
                  <tr key={tx.id}>
                    <td>
                      <span className="badge badge-primary">{tx.type}</span>
                    </td>
                    <td className={tx.amount >= 0 ? "positive" : "negative"}>
                      {tx.amount >= 0 ? "+" : ""}
                      {tx.amount}
                    </td>
                    <td>{tx.balanceAfter}</td>
                    <td>{tx.description || "—"}</td>
                    <td>{formatDate(tx.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="card animate-fadeIn">
          <h3>Payments</h3>
          {payments.length === 0 ? (
            <div className="empty-state">
              <p>No payments recorded.</p>
            </div>
          ) : (
            <table className="billing-table">
              <thead>
                <tr>
                  <th>Provider</th>
                  <th>Amount</th>
                  <th>Credits</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id}>
                    <td>{p.provider}</td>
                    <td>${(p.amountCents / 100).toFixed(2)}</td>
                    <td>{p.creditsGranted}</td>
                    <td>
                      <span
                        className={`badge ${p.status === "COMPLETED" ? "badge-success" : "badge-warning"}`}
                      >
                        {p.status}
                      </span>
                    </td>
                    <td>{formatDate(p.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
      <UpiQrModal
        show={showUpiQr}
        onClose={() => setShowUpiQr(false)}
        qrData={upiQrData}
      />
    </AppLayout>
  );
}

function formatDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString();
}
