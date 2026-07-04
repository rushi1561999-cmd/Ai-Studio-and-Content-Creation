package com.example.demo.service;

import com.example.demo.dto.StripeCheckoutResponse;
import com.example.demo.entity.Payment;
import com.example.demo.entity.StripePayment;
import com.example.demo.enums.CreditPack;
import com.example.demo.enums.CreditTransactionType;
import com.example.demo.enums.PaymentProvider;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.StripePaymentRepository;
import com.example.demo.service.billing.WalletBillingService;
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

@Service
public class StripeService {

    private final WalletBillingService walletBillingService;
    private final StripePaymentRepository stripePaymentRepository;
    private final PaymentRepository paymentRepository;
    private final WorkspaceAccessService workspaceAccessService;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public StripeService(
            WalletBillingService walletBillingService,
            StripePaymentRepository stripePaymentRepository,
            PaymentRepository paymentRepository,
            WorkspaceAccessService workspaceAccessService) {
        this.walletBillingService = walletBillingService;
        this.stripePaymentRepository = stripePaymentRepository;
        this.paymentRepository = paymentRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @PostConstruct
    void init() {
        if (stripeApiKey != null && !stripeApiKey.isBlank()) {
            Stripe.apiKey = stripeApiKey.trim();
        }
    }

    public boolean isConfigured() {
        return stripeApiKey != null && !stripeApiKey.isBlank();
    }

    public StripeCheckoutResponse createCheckoutSession(String workspaceId, String packId) {
        requireConfigured();
        workspaceAccessService.requireWorkspaceAccess(workspaceId);

        CreditPack pack = CreditPack.fromId(packId);

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/wallet?payment=success")
                    .setCancelUrl(frontendUrl + "/wallet?payment=cancelled")
                    .putMetadata("workspaceId", workspaceId)
                    .putMetadata("credits", String.valueOf(pack.getCredits()))
                    .putMetadata("pack", pack.getId())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(pack.getPriceCents())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(pack.getLabel())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            return new StripeCheckoutResponse(session.getUrl(), session.getId());

        } catch (StripeException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Stripe checkout failed: " + e.getMessage()
            );
        }
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        requireConfigured();

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe webhook secret is not configured."
            );
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret.trim());
        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature.");
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid session payload."));

        if (!"paid".equals(session.getPaymentStatus())) {
            return;
        }

        if (stripePaymentRepository.existsById(session.getId())) {
            return;
        }

        String workspaceId = session.getMetadata().get("workspaceId");
        String creditsStr = session.getMetadata().get("credits");

        if (workspaceId == null || creditsStr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing payment metadata.");
        }

        int credits = Integer.parseInt(creditsStr);
        int amountCents = session.getAmountTotal() != null ? session.getAmountTotal().intValue() : 0;

        walletBillingService.credit(workspaceId, credits, CreditTransactionType.PURCHASE,
                session.getId(), "Stripe credit purchase");

        Payment payment = new Payment();
        payment.setWorkspaceId(workspaceId);
        payment.setAmountCents(amountCents);
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setExternalId(session.getId());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCreditsGranted(credits);
        paymentRepository.save(payment);

        StripePayment stripePayment = new StripePayment();
        stripePayment.setSessionId(session.getId());
        stripePayment.setWorkspaceId(workspaceId);
        stripePayment.setCredits(credits);
        stripePaymentRepository.save(stripePayment);
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe is not configured. Set stripe.api.key in application-local.properties."
            );
        }
    }
}
