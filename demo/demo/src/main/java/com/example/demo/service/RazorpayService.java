package com.example.demo.service;

import com.example.demo.dto.RazorpayCheckoutResponse;
import com.example.demo.entity.Payment;
import com.example.demo.entity.RazorpayPayment;
import com.example.demo.entity.Subscription;
import com.example.demo.entity.SubscriptionPlan;
import com.example.demo.enums.CreditPack;
import com.example.demo.enums.PaymentProvider;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RazorpayPaymentRepository;
import com.example.demo.repository.SubscriptionPlanRepository;
import com.example.demo.repository.SubscriptionRepository;
import com.example.demo.service.billing.PaymentFulfillmentService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class RazorpayService {

    private static final Logger LOGGER = Logger.getLogger(RazorpayService.class.getName());
    private static final String RAZORPAY_API_URL = "https://api.razorpay.com/v1";

    private final PaymentFulfillmentService paymentFulfillmentService;
    private final RazorpayPaymentRepository razorpayPaymentRepository;
    private final PaymentRepository paymentRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final RestTemplate providerRestTemplate;

    @Value("${razorpay.api.key:}")
    private String razorpayApiKey;

    @Value("${razorpay.api.secret:}")
    private String razorpayApiSecret;

    @Value("${razorpay.webhook.secret:}")
    private String razorpayWebhookSecret;

    public RazorpayService(
            PaymentFulfillmentService paymentFulfillmentService,
            RazorpayPaymentRepository razorpayPaymentRepository,
            PaymentRepository paymentRepository,
            WorkspaceAccessService workspaceAccessService,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            RestTemplate providerRestTemplate) {
        this.paymentFulfillmentService = paymentFulfillmentService;
        this.razorpayPaymentRepository = razorpayPaymentRepository;
        this.paymentRepository = paymentRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.providerRestTemplate = providerRestTemplate;
    }

    public boolean isConfigured() {
        return hasText(razorpayApiKey)
                && hasText(razorpayApiSecret)
                && hasText(razorpayWebhookSecret);
    }

    public RazorpayCheckoutResponse createOrder(
            String workspaceId,
            String packId,
            String subscriptionId,
            String userEmail) {
        requireConfigured();
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        ChargeDetails charge = resolveCharge(workspaceId, packId, subscriptionId);

        JSONObject orderData = new JSONObject();
        orderData.put("amount", charge.amountMinor());
        orderData.put("currency", charge.currency());
        orderData.put("receipt", "ai_" + UUID.randomUUID().toString().replace("-", ""));

        JSONObject notes = new JSONObject();
        notes.put("workspaceId", workspaceId);
        notes.put("credits", charge.credits());
        notes.put("purchaseType", charge.subscriptionId() == null ? "CREDIT_PACK" : "SUBSCRIPTION");
        notes.put("userEmail", userEmail);
        if (charge.subscriptionId() != null) {
            notes.put("subscriptionId", charge.subscriptionId());
        } else {
            notes.put("pack", packId);
        }
        orderData.put("notes", notes);

        JSONObject response = makeApiRequest(HttpMethod.POST, "/orders", orderData.toString());
        String orderId = response.getString("id");

        Payment payment = new Payment();
        payment.setWorkspaceId(workspaceId);
        payment.setSubscriptionId(charge.subscriptionId());
        payment.setAmountCents(charge.amountMinor());
        payment.setCurrency(charge.currency());
        payment.setProvider(PaymentProvider.RAZORPAY);
        payment.setExternalId(orderId);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreditsGranted(charge.credits());
        paymentRepository.save(payment);

        return new RazorpayCheckoutResponse(
                orderId,
                razorpayApiKey.trim(),
                String.valueOf(charge.amountMinor()),
                charge.currency(),
                userEmail,
                "AI Studio",
                charge.description());
    }

    @Transactional
    public Payment verifyPayment(String orderId, String paymentId, String signature) {
        requireConfigured();
        requireProviderId(orderId, "order");
        requireProviderId(paymentId, "payment");

        Payment localPayment = paymentRepository.findByProviderAndExternalId(PaymentProvider.RAZORPAY, orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found."));
        workspaceAccessService.requireWorkspaceAccess(localPayment.getWorkspaceId());

        String expectedSignature = hmacHex(orderId + "|" + paymentId, razorpayApiSecret);
        if (!secureEquals(expectedSignature, signature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay payment signature.");
        }

        JSONObject paymentData = makeApiRequest(HttpMethod.GET, "/payments/" + paymentId, null);
        if (!orderId.equals(paymentData.optString("order_id"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment does not belong to this order.");
        }
        if (!"captured".equals(paymentData.optString("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay payment has not been captured.");
        }

        JSONObject orderData = makeApiRequest(HttpMethod.GET, "/orders/" + orderId, null);
        int amount = orderData.getInt("amount");
        String currency = orderData.getString("currency");
        if (paymentData.optInt("amount", -1) != amount
                || !currency.equalsIgnoreCase(paymentData.optString("currency"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay order and payment totals do not match.");
        }

        Payment payment = paymentFulfillmentService.fulfill(
                PaymentProvider.RAZORPAY,
                orderId,
                paymentId,
                amount,
                currency);
        saveProviderReceipt(payment, orderId, paymentId);
        return payment;
    }

    @Transactional
    public void processWebhook(String payload, String webhookSignature) {
        requireWebhookConfigured();
        String expectedSignature = hmacHex(payload, razorpayWebhookSecret);
        if (!secureEquals(expectedSignature, webhookSignature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay webhook signature.");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.optString("event");
        if (!"payment.captured".equals(eventType)
                && !"order.paid".equals(eventType)
                && !"payment.failed".equals(eventType)) {
            return;
        }

        JSONObject paymentData = event.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");
        String orderId = paymentData.optString("order_id");
        String paymentId = paymentData.optString("id");
        if (orderId.isBlank() || paymentId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay webhook payment identifiers are missing.");
        }

        if ("payment.failed".equals(eventType)) {
            paymentFulfillmentService.markFailed(PaymentProvider.RAZORPAY, orderId, paymentId);
            return;
        }
        if (!"captured".equals(paymentData.optString("status"))) {
            return;
        }

        Payment payment = paymentFulfillmentService.fulfill(
                PaymentProvider.RAZORPAY,
                orderId,
                paymentId,
                paymentData.getInt("amount"),
                paymentData.getString("currency"));
        saveProviderReceipt(payment, orderId, paymentId);
    }

    private void saveProviderReceipt(Payment payment, String orderId, String paymentId) {
        if (razorpayPaymentRepository.findByOrderId(orderId).isEmpty()) {
            razorpayPaymentRepository.save(new RazorpayPayment(
                    orderId,
                    payment.getWorkspaceId(),
                    paymentId,
                    payment.getCreditsGranted()));
        }
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
                    plan.getCurrency().toUpperCase(Locale.ROOT),
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

    private JSONObject makeApiRequest(HttpMethod method, String endpoint, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(razorpayApiKey.trim(), razorpayApiSecret.trim(), StandardCharsets.UTF_8);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = providerRestTemplate.exchange(
                    RAZORPAY_API_URL + endpoint,
                    method,
                    entity,
                    String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay returned an empty response.");
            }
            return new JSONObject(response.getBody());
        } catch (RestClientResponseException exception) {
            LOGGER.log(Level.WARNING, "Razorpay API request failed with status {0}", exception.getStatusCode().value());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay could not process the request.");
        } catch (RestClientException exception) {
            LOGGER.log(Level.WARNING, "Razorpay API request could not be completed", exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay is temporarily unreachable.");
        }
    }

    private String hmacHex(String text, String secretValue) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretValue.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not validate payment signature.", exception);
        }
    }

    private boolean secureEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.trim().getBytes(StandardCharsets.UTF_8));
    }

    private void requireProviderId(String value, String label) {
        if (value == null || !value.matches("[A-Za-z0-9_]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay " + label + " identifier.");
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay checkout is unavailable until API and webhook secrets are configured.");
        }
    }

    private void requireWebhookConfigured() {
        if (!hasText(razorpayWebhookSecret)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Razorpay webhook secret is not configured.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ChargeDetails(
            int amountMinor,
            String currency,
            int credits,
            String description,
            String subscriptionId) {}
}
