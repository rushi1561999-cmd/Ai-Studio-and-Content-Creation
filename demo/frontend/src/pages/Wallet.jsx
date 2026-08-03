import { useEffect, useState } from "react";
import { useSearchParams } from "react-router";
import api from "../api/axiosConfig";
import AppLayout from "../components/AppLayout";
import Icon from "../components/Icon";
import { useWorkspace } from "../context/workspace-context";
import "./Wallet.css";

const PACKS = {
  Starter: {
    pack: "starter",
    credits: 100,
    priceMinor: 9000,
    gradient: "var(--primary-gradient)",
    badge: "S",
  },
  Professional: {
    pack: "professional",
    credits: 500,
    priceMinor: 29900,
    gradient: "var(--accent-gradient)",
    badge: "P",
  },
  Enterprise: {
    pack: "enterprise",
    credits: 1000,
    priceMinor: 79900,
    gradient: "var(--success-gradient)",
    badge: "E",
  },
};

export default function Wallet() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { workspaceId, credits, refreshWallet } = useWorkspace();
  const [stripeEnabled, setStripeEnabled] = useState(false);
  const [razorpayEnabled, setRazorpayEnabled] = useState(false);
  const [plans, setPlans] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [payments, setPayments] = useState([]);
  const [subscription, setSubscription] = useState(null);
  const [selectedPack, setSelectedPack] = useState(null);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [purchasing, setPurchasing] = useState(false);
  const [subscribing, setSubscribing] = useState(false);
  const [paymentNotice, setPaymentNotice] = useState("");

  const gatewayAvailable = razorpayEnabled || stripeEnabled;
  const gatewayName = razorpayEnabled
    ? "Razorpay"
    : stripeEnabled
      ? "Stripe"
      : null;

  useEffect(() => {
    let cancelled = false;
    Promise.allSettled([api.get("/stripe/status"), api.get("/razorpay/status")]).then(
      ([stripe, razorpay]) => {
        if (cancelled) return;
        setStripeEnabled(
          stripe.status === "fulfilled" && Boolean(stripe.value.data.enabled),
        );
        setRazorpayEnabled(
          razorpay.status === "fulfilled" && Boolean(razorpay.value.data.enabled),
        );
      },
    );
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    api
      .get("/billing/plans")
      .then(({ data }) => {
        if (!cancelled) setPlans(data);
      })
      .catch((error) => {
        if (!cancelled) {
          setPaymentNotice(
            error.response?.data?.message || "Could not load billing plans.",
          );
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!workspaceId) return undefined;
    let cancelled = false;
    Promise.all([
      api.get(`/billing/workspace/${workspaceId}/transactions`),
      api.get(`/billing/workspace/${workspaceId}/payments`),
      api.get(`/billing/workspace/${workspaceId}/subscription`),
    ])
      .then(([txResponse, paymentResponse, subscriptionResponse]) => {
        if (cancelled) return;
        setTransactions(txResponse.data);
        setPayments(paymentResponse.data);
        setSubscription(subscriptionResponse.data || null);
      })
      .catch((error) => {
        if (!cancelled) {
          setPaymentNotice(
            error.response?.data?.message || "Could not load billing history.",
          );
        }
      });
    refreshWallet();
    return () => {
      cancelled = true;
    };
  }, [workspaceId, refreshWallet]);

  useEffect(() => {
    const paymentResult = searchParams.get("payment");
    if (!paymentResult) return undefined;
    const timer = window.setTimeout(() => {
      if (paymentResult === "success") {
        setPaymentNotice(
          "Payment received. Your balance updates after secure provider verification.",
        );
        refreshWallet();
      } else if (paymentResult === "cancelled") {
        setPaymentNotice("Payment was cancelled. No charge was fulfilled.");
      }
      setSearchParams({});
    }, 0);
    return () => window.clearTimeout(timer);
  }, [searchParams, setSearchParams, refreshWallet]);

  const reloadBilling = async () => {
    if (!workspaceId) return;
    const [txResponse, paymentResponse, subscriptionResponse] =
      await Promise.all([
        api.get(`/billing/workspace/${workspaceId}/transactions`),
        api.get(`/billing/workspace/${workspaceId}/payments`),
        api.get(`/billing/workspace/${workspaceId}/subscription`),
      ]);
    setTransactions(txResponse.data);
    setPayments(paymentResponse.data);
    setSubscription(subscriptionResponse.data || null);
  };

  const verifyRazorpay = async (response) => {
    await api.post("/razorpay/verify", {
      orderId: response.razorpay_order_id,
      paymentId: response.razorpay_payment_id,
      signature: response.razorpay_signature,
    });
    await Promise.all([refreshWallet(), reloadBilling()]);
  };

  const openRazorpay = (checkout, description, onComplete, onDismiss) => {
    if (!window.Razorpay) {
      throw new Error("Razorpay Checkout could not load. Refresh and try again.");
    }
    const razorpay = new window.Razorpay({
      key: checkout.key,
      amount: checkout.amount,
      currency: checkout.currency,
      order_id: checkout.orderId,
      name: "AI Studio",
      description,
      prefill: { email: checkout.email },
      theme: { color: "#8b5cf6" },
      handler: onComplete,
      modal: { ondismiss: onDismiss },
    });
    razorpay.open();
  };

  const handlePurchase = async (packName) => {
    const pack = PACKS[packName];
    if (!workspaceId || !pack || !gatewayAvailable) return;
    setPurchasing(true);
    setSelectedPack(packName);
    setPaymentNotice("");

    try {
      if (razorpayEnabled) {
        const { data } = await api.post(
          `/razorpay/order?workspaceId=${workspaceId}&pack=${pack.pack}`,
        );
        openRazorpay(
          data,
          `${pack.credits} AI credits`,
          async (response) => {
            try {
              await verifyRazorpay(response);
              setPaymentNotice(
                `Payment verified. ${pack.credits} credits were added exactly once.`,
              );
            } catch (error) {
              setPaymentNotice(
                error.response?.data?.message ||
                  "Payment verification failed. Contact support with your payment ID.",
              );
            } finally {
              setPurchasing(false);
              setSelectedPack(null);
            }
          },
          () => {
            setPaymentNotice("Payment was cancelled.");
            setPurchasing(false);
            setSelectedPack(null);
          },
        );
        return;
      }

      const { data } = await api.post(
        `/stripe/checkout?workspaceId=${workspaceId}&pack=${pack.pack}`,
      );
      window.location.assign(data.checkoutUrl);
    } catch (error) {
      setPaymentNotice(
        error.response?.data?.message || error.message || "Checkout failed.",
      );
      setPurchasing(false);
      setSelectedPack(null);
    }
  };

  const handleSubscribe = async (plan) => {
    if (!workspaceId || !plan || !gatewayAvailable) return;
    setSubscribing(true);
    setSelectedPlan(plan);
    setPaymentNotice("");

    try {
      if (razorpayEnabled && !window.Razorpay) {
        throw new Error("Razorpay Checkout could not load. Refresh and try again.");
      }
      const { data: pending } = await api.post(
        `/billing/workspace/${workspaceId}/subscribe`,
        { planId: plan.id },
      );

      if (razorpayEnabled) {
        const { data } = await api.post(
          `/razorpay/order?workspaceId=${workspaceId}&subscriptionId=${pending.id}`,
        );
        openRazorpay(
          data,
          `${plan.name} â€” 30-day plan`,
          async (response) => {
            try {
              await verifyRazorpay(response);
              setPaymentNotice("Payment verified and plan activated for 30 days.");
            } catch (error) {
              setPaymentNotice(
                error.response?.data?.message ||
                  "Payment verification failed. Contact support with your payment ID.",
              );
            } finally {
              setSubscribing(false);
              setSelectedPlan(null);
            }
          },
          () => {
            setPaymentNotice("Payment was cancelled; the plan was not activated.");
            setSubscribing(false);
            setSelectedPlan(null);
          },
        );
        return;
      }

      const { data } = await api.post(
        `/stripe/checkout?workspaceId=${workspaceId}&subscriptionId=${pending.id}`,
      );
      window.location.assign(data.checkoutUrl);
    } catch (error) {
      setPaymentNotice(
        error.response?.data?.message || error.message || "Subscription checkout failed.",
      );
      setSubscribing(false);
      setSelectedPlan(null);
    }
  };

  const handleCancelSubscription = async () => {
    if (!workspaceId || !subscription?.id) return;
    if (!window.confirm("Cancel this plan now? Remaining wallet credits stay available.")) {
      return;
    }
    try {
      await api.post(
        `/billing/workspace/${workspaceId}/subscription/${subscription.id}/cancel`,
      );
      setSubscription(null);
      setPaymentNotice("Plan cancelled. Existing wallet credits were preserved.");
    } catch (error) {
      setPaymentNotice(
        error.response?.data?.message || "Could not cancel the plan.",
      );
    }
  };

  return (
    <AppLayout
      title="Billing and usage"
      subtitle="Verified payments, an auditable wallet ledger, and paid 30-day plans."
    >
      {paymentNotice && (
        <div
          className={`alert ${/verified|activated|preserved/i.test(paymentNotice) ? "alert-success" : "alert-warning"}`}
        >
          {paymentNotice}
        </div>
      )}

      {!gatewayAvailable && (
        <div className="alert alert-warning">
          Payments are unavailable until an administrator configures Razorpay or
          Stripe API and webhook secrets. Manual wallet top-ups are disabled.
        </div>
      )}

      <div className="balance-banner card animate-fadeIn">
        <div className="balance-icon gradient-bg">
          <Icon name="bolt" size={24} />
        </div>
        <div className="balance-info">
          <h3>Available balance</h3>
          <h1 className="gradient-text">{credits} credits</h1>
          <p>
            {gatewayName
              ? `Secure checkout is connected through ${gatewayName}.`
              : "Generation remains available while your current credit balance lasts."}
          </p>
        </div>
      </div>

      {plans.length > 0 && (
        <section className="billing-section animate-fadeIn">
          <div className="section-header">
            <h2>Paid 30-day plans</h2>
          </div>
          {subscription ? (
            <div className="subscription-active card">
              <div className="subscription-content">
                <h4>Active plan</h4>
                <p className="subscription-plan">{subscription.planName}</p>
                <p className="subscription-details">
                  {subscription.monthlyCredits} credits Â· {subscription.status}
                </p>
                <p className="subscription-date">
                  Valid until: {formatDate(subscription.renewalDate)}
                </p>
              </div>
              <button className="btn btn-danger" onClick={handleCancelSubscription}>
                Cancel plan
              </button>
            </div>
          ) : (
            <div className="plans-selection">
              {plans.map((plan) => (
                <div
                  key={plan.id}
                  className={`plan-card card ${selectedPlan?.id === plan.id ? "selected" : ""}`}
                >
                  <div className="plan-header">
                    <h4>{plan.name}</h4>
                    <span className="plan-status-badge">30 days</span>
                  </div>
                  <p className="plan-price">
                    {formatMoney(plan.priceCents, plan.currency)}
                  </p>
                  <p className="plan-credits">
                    {plan.monthlyCredits} credits on activation
                  </p>
                  <ul className="plan-features">
                    <li>Verified provider checkout</li>
                    <li>Auditable wallet transaction</li>
                    <li>AI generation and marketplace access</li>
                  </ul>
                  <button
                    className="btn btn-primary"
                    disabled={subscribing || !workspaceId || !gatewayAvailable}
                    onClick={() => handleSubscribe(plan)}
                  >
                    {subscribing && selectedPlan?.id === plan.id
                      ? "Opening checkout..."
                      : gatewayAvailable
                        ? `Buy ${plan.name}`
                        : "Payments unavailable"}
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      <h2 className="section-title">One-time credit packs</h2>
      <div className="pricing-grid">
        {Object.entries(PACKS).map(([name, pack]) => (
          <div
            key={name}
            className={`pricing-card card hover-lift ${name === "Professional" ? "popular" : ""}`}
          >
            {name === "Professional" && (
              <div className="popular-badge badge badge-warning">Popular</div>
            )}
            <div className="pricing-header" style={{ background: pack.gradient }}>
              <div className="pack-emoji">{pack.badge}</div>
              <h3>{name}</h3>
            </div>
            <h2>
              {formatMoney(pack.priceMinor, "INR")}
              <span>/pack</span>
            </h2>
            <ul>
              <li>{pack.credits} generation credits</li>
              <li>One-time verified payment</li>
              <li>No automatic charge</li>
            </ul>
            <button
              type="button"
              className="btn btn-primary"
              disabled={purchasing || !workspaceId || !gatewayAvailable}
              onClick={() => handlePurchase(name)}
            >
              {purchasing && selectedPack === name
                ? "Opening checkout..."
                : gatewayAvailable
                  ? `Pay with ${gatewayName}`
                  : "Payments unavailable"}
            </button>
          </div>
        ))}
      </div>

      <div className="billing-tables">
        <BillingTable
          title="Credit transactions"
          empty="No wallet transactions yet."
          headers={["Type", "Amount", "Balance", "Description", "Date"]}
          rows={transactions.map((transaction) => [
            transaction.type,
            `${transaction.amount >= 0 ? "+" : ""}${transaction.amount}`,
            transaction.balanceAfter,
            transaction.description || "â€”",
            formatDate(transaction.createdAt),
          ])}
        />
        <BillingTable
          title="Payments"
          empty="No provider payments recorded."
          headers={["Provider", "Amount", "Credits", "Status", "Date"]}
          rows={payments.map((payment) => [
            payment.provider,
            formatMoney(payment.amountCents, payment.currency),
            payment.creditsGranted,
            payment.status,
            formatDate(payment.createdAt),
          ])}
        />
      </div>
    </AppLayout>
  );
}

function BillingTable({ title, empty, headers, rows }) {
  return (
    <section className="card animate-fadeIn">
      <h3>{title}</h3>
      {rows.length === 0 ? (
        <div className="empty-state"><p>{empty}</p></div>
      ) : (
        <table className="billing-table">
          <thead><tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr></thead>
          <tbody>
            {rows.map((row, rowIndex) => (
              <tr key={`${title}-${rowIndex}`}>
                {row.map((value, columnIndex) => <td key={`${rowIndex}-${columnIndex}`}>{value}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function formatMoney(amountMinor, currency = "INR") {
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: currency || "INR",
  }).format(Number(amountMinor || 0) / 100);
}

function formatDate(value) {
  if (!value) return "â€”";
  return new Date(value).toLocaleString();
}
