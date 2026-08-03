package com.example.demo.service;

import com.example.demo.dto.StripeCheckoutResponse;
import com.example.demo.entity.Payment;
import com.example.demo.entity.StripePayment;
import com.example.demo.entity.Subscription;
import com.example.demo.entity.SubscriptionPlan;
import com.example.demo.enums.CreditPack;
import com.example.demo.enums.PaymentProvider;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.StripePaymentRepository;
import com.example.demo.repository.SubscriptionPlanRepository;
import com.example.demo.repository.SubscriptionRepository;
import com.example.demo.service.billing.PaymentFulfillmentService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class StripeService {

    private final PaymentFulfillmentService paymentFulfillmentService;
    private final StripePaymentRepository stripePaymentRepository;
    private final PaymentRepository paymentRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public StripeService(
            PaymentFulfillmentService paymentFulfillmentService,
            StripePaymentRepository stripePaymentRepository,
            PaymentRepository paymentRepository,
            WorkspaceAccessService workspaceAccessService,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository) {
        this.paymentFulfillmentService = paymentFulfillmentService;
        this.stripePaymentRepository = stripePaymentRepository;
        this.paymentRepository = paymentRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @PostConstruct
    void init() {
        if (stripeApiKey != null && !stripeApiKey.isBlank()) {
            Stripe.apiKey = stripeApiKey.trim();
        }
    }

    public boolean isConfigured() {
        return stripeApiKey != null && !stripeApiKey.isBlank()
                && webhookSecret != null && !webhookSecret.isBlank();
    }

    public StripeCheckoutResponse createCheckoutSession(
            String workspaceId,
            String packId,
            String subscriptionId) {
        requireConfigured();
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        ChargeDetails charge = resolveCharge(workspaceId, packId, subscriptionId);

        try {
            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/wallet?payment=success&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/wallet?payment=cancelled")
                    .putMetadata("workspaceId", workspaceId)
                    .putMetadata("credits", String.valueOf(charge.credits()))
                    .putMetadata("purchaseType", charge.subscriptionId() == null ? "CREDIT_PACK" : "SUBSCRIPTION")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(charge.currency().toLowerCase(Locale.ROOT))
                                    .setUnitAmount((long) charge.amountMinor())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(charge.description())
                                            .build())
                                    .build())
                            .build());
            if (charge.subscriptionId() != null) {
                builder.putMetadata("subscriptionId", charge.subscriptionId());
            }

            Session session = Session.create(builder.build());

            Payment payment = new Payment();
            payment.setWorkspaceId(workspaceId);
            payment.setSubscriptionId(charge.subscriptionId());
            payment.setAmountCents(charge.amountMinor());
            payment.setCurrency(charge.currency());
            payment.setProvider(PaymentProvider.STRIPE);
            payment.setExternalId(session.getId());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreditsGranted(charge.credits());
            paymentRepository.save(payment);

            return new StripeCheckoutResponse(session.getUrl(), session.getId());
        } catch (StripeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe checkout could not be created.");
        }
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        requireConfigured();

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret.trim());
        } catch (SignatureVerificationException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature.");
        }

        if ("checkout.session.async_payment_failed".equals(event.getType())) {
            sessionFrom(event).ifPresent(session -> paymentFulfillmentService.markFailed(
                    PaymentProvider.STRIPE,
                    session.getId(),
                    session.getPaymentIntent()));
            return;
        }
        if (!"checkout.session.completed".equals(event.getType())
                && !"checkout.session.async_payment_succeeded".equals(event.getType())) {
            return;
        }

        Session session = sessionFrom(event)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe session payload."));
        if (!"paid".equals(session.getPaymentStatus())) {
            return;
        }
        if (session.getAmountTotal() == null || session.getCurrency() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe payment totals are missing.");
        }

        Payment payment = paymentFulfillmentService.fulfill(
                PaymentProvider.STRIPE,
                session.getId(),
                session.getPaymentIntent(),
                Math.toIntExact(session.getAmountTotal()),
                session.getCurrency());

        if (!stripePaymentRepository.existsById(session.getId())) {
            StripePayment stripePayment = new StripePayment();
            stripePayment.setSessionId(session.getId());
            stripePayment.setWorkspaceId(payment.getWorkspaceId());
            stripePayment.setCredits(payment.getCreditsGranted());
            stripePaymentRepository.save(stripePayment);
        }
    }

    private java.util.Optional<Session> sessionFrom(Event event) {
        return event.getDataObjectDeserializer().getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast);
    }

    private ChargeDetails resolveCharge(String workspaceId, String packId, String subscriptionId) {
        if (subscriptionId != null && !subscriptionId.isBlank()) {
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found."));
            if (!workspaceId.equals(subscription.getWorkspaceId()) || !"PENDING".equals(subscription.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Subscription is not awaiting payment for this workspace.");
            }
            SubscriptionPlan plan = subscriptionPlanRepository.findById(subscription.getPlanId())
                    .filter(SubscriptionPlan::isActive)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription plan not found."));
            return new ChargeDetails(
                    plan.getPriceCents(),
                    plan.getCurrency(),
                    plan.getMonthlyCredits(),
                    plan.getName() + " — 30-day plan",
                    subscription.getId());
        }
        if (packId == null || packId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit pack or subscription is required.");
        }
        CreditPack pack = CreditPack.fromId(packId);
        return new ChargeDetails(
                Math.toIntExact(pack.getPriceCents()),
                CreditPack.CURRENCY,
                pack.getCredits(),
                pack.getLabel(),
                null);
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe checkout is unavailable until both API and webhook secrets are configured.");
        }
    }

    private record ChargeDetails(
            int amountMinor,
            String currency,
            int credits,
            String description,
            String subscriptionId) {}
}
