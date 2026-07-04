package com.example.demo.service;

import com.example.demo.dto.RazorpayCheckoutResponse;
import com.example.demo.dto.UpiQrResponse;
import com.example.demo.entity.RazorpayPayment;
import com.example.demo.enums.CreditPack;
import com.example.demo.enums.CreditTransactionType;
import com.example.demo.enums.PaymentProvider;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RazorpayPaymentRepository;
import com.example.demo.service.billing.WalletBillingService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class RazorpayService {

    private static final Logger logger = Logger.getLogger(RazorpayService.class.getName());
    private static final String RAZORPAY_API_URL = "https://api.razorpay.com/v1";

    private final WalletBillingService walletBillingService;
    private final RazorpayPaymentRepository razorpayPaymentRepository;
    private final PaymentRepository paymentRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final com.example.demo.repository.SubscriptionPlanRepository subscriptionPlanRepository;

    @Value("${razorpay.api.key:}")
    private String razorpayApiKey;

    @Value("${razorpay.api.secret:}")
    private String razorpayApiSecret;

    @Value("${razorpay.webhook.secret:}")
    private String razorpayWebhookSecret;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${upi.vpa:}")
    private String upiVpa;

    @Value("${upi.payee.name:AI Studio}")
    private String upiPayeeName;

    public RazorpayService(
            WalletBillingService walletBillingService,
            RazorpayPaymentRepository razorpayPaymentRepository,
            PaymentRepository paymentRepository,
            WorkspaceAccessService workspaceAccessService,
            com.example.demo.repository.SubscriptionPlanRepository subscriptionPlanRepository) {
        this.walletBillingService = walletBillingService;
        this.razorpayPaymentRepository = razorpayPaymentRepository;
        this.paymentRepository = paymentRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @PostConstruct
    void init() {
        if (razorpayApiKey != null && !razorpayApiKey.isBlank() 
            && razorpayApiSecret != null && !razorpayApiSecret.isBlank()) {
            logger.info("[Razorpay] Configuration loaded successfully");
        }
    }

    public boolean isConfigured() {
        return razorpayApiKey != null && !razorpayApiKey.isBlank() 
            && razorpayApiSecret != null && !razorpayApiSecret.isBlank();
    }

    public boolean isUpiConfigured() {
        return upiVpa != null && !upiVpa.isBlank();
    }

    public RazorpayCheckoutResponse createOrder(String workspaceId, String packId, String userEmail) {
        requireConfigured();
        workspaceAccessService.requireWorkspaceAccess(workspaceId);

        CreditPack pack = CreditPack.fromId(packId);

        try {
            JSONObject orderData = new JSONObject();
            orderData.put("amount", pack.getPriceCents());
            orderData.put("currency", "INR");
            orderData.put("receipt", "order_" + System.currentTimeMillis());
            
            JSONObject notes = new JSONObject();
            notes.put("workspaceId", workspaceId);
            notes.put("credits", pack.getCredits());
            notes.put("pack", pack.getId());
            notes.put("userEmail", userEmail);
            orderData.put("notes", notes);

            JSONObject response = makeApiRequest("POST", "/orders", orderData.toString());
            String orderId = response.getString("id");

            logger.info("[Razorpay] Order created: " + orderId + " for workspace: " + workspaceId);

            RazorpayCheckoutResponse checkoutResponse = new RazorpayCheckoutResponse(
                    orderId,
                    razorpayApiKey.trim(),
                    String.valueOf(pack.getPriceCents()),
                    "INR",
                    userEmail,
                    pack.getLabel(),
                    "Credit Purchase - " + pack.getLabel()
            );

            return checkoutResponse;

        } catch (Exception e) {
            logger.severe("[Razorpay] Failed to create order: " + e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Razorpay order creation failed: " + e.getMessage()
            );
        }
    }

    @Transactional
    public void verifyPayment(String orderId, String paymentId, String signature) {
        requireConfigured();

        try {
            // Verify payment signature
            String text = orderId + "|" + paymentId;
            String expectedSignature = generateSignature(text);

            if (!expectedSignature.equals(signature)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid payment signature"
                );
            }

            // Check if payment already processed
            if (razorpayPaymentRepository.findByPaymentId(paymentId).isPresent()) {
                logger.warning("[Razorpay] Payment already processed: " + paymentId);
                return;
            }

            // Fetch payment details from Razorpay
            JSONObject paymentData = makeApiRequest("GET", "/payments/" + paymentId, null);
            String paymentStatus = paymentData.getString("status");

            if (!"captured".equals(paymentStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment not captured"
                );
            }

            // Fetch order to get metadata
            JSONObject orderData = makeApiRequest("GET", "/orders/" + orderId, null);
            JSONObject notes = orderData.getJSONObject("notes");
            
            String workspaceId = notes.getString("workspaceId");
            int credits = notes.getInt("credits");
            int amountPaise = orderData.getInt("amount");

            // Credit the wallet
            walletBillingService.credit(
                    workspaceId,
                    credits,
                    CreditTransactionType.PURCHASE,
                    paymentId,
                    "Razorpay credit purchase"
            );

            // Save payment record
            com.example.demo.entity.Payment paymentRecord = new com.example.demo.entity.Payment();
            paymentRecord.setWorkspaceId(workspaceId);
            paymentRecord.setAmountCents(amountPaise);
            paymentRecord.setProvider(PaymentProvider.RAZORPAY);
            paymentRecord.setExternalId(paymentId);
            paymentRecord.setStatus(PaymentStatus.COMPLETED);
            paymentRecord.setCreditsGranted(credits);
            paymentRepository.save(paymentRecord);

            // Save Razorpay payment record
            RazorpayPayment razorpayPayment = new RazorpayPayment(
                    orderId,
                    workspaceId,
                    paymentId,
                    credits
            );
            razorpayPaymentRepository.save(razorpayPayment);

            logger.info("[Razorpay] Payment verified and credited: " + paymentId);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.severe("[Razorpay] Failed to verify payment: " + e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment verification failed: " + e.getMessage()
            );
        }
    }

    private JSONObject makeApiRequest(String method, String endpoint, String body) throws Exception {
        String auth = Base64.getEncoder().encodeToString(
                (razorpayApiKey.trim() + ":" + razorpayApiSecret.trim()).getBytes(StandardCharsets.UTF_8)
        );

        URL url = URI.create(RAZORPAY_API_URL + endpoint).toURL();
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Basic " + auth);
        conn.setRequestProperty("Content-Type", "application/json");

        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("Razorpay API error (HTTP " + responseCode + "): " + response.toString());
        }

        return new JSONObject(response.toString());
    }

    private String generateSignature(String text) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secret = new javax.crypto.spec.SecretKeySpec(
                    razorpayApiSecret.trim().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secret);
            byte[] digest = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.severe("[Razorpay] Failed to generate signature: " + e.getMessage());
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay is not configured. Set razorpay.api.key and razorpay.api.secret in application-local.properties."
            );
        }
    }

    public UpiQrResponse generateUpiQrCode(String workspaceId, String packId, String userEmail) {
        requireConfigured();
        workspaceAccessService.requireWorkspaceAccess(workspaceId);

        // Try to get subscription plan from database first, fallback to CreditPack enum
        int credits;
        int priceCents;
        String planName;

        try {
            com.example.demo.entity.SubscriptionPlan plan = subscriptionPlanRepository.findById(packId)
                    .orElse(null);
            if (plan != null && plan.isActive()) {
                credits = plan.getMonthlyCredits();
                priceCents = plan.getPriceCents();
                planName = plan.getName();
                logger.info("[Razorpay] Using subscription plan from database: " + planName);
            } else {
                // Fallback to CreditPack enum
                CreditPack pack = CreditPack.fromId(packId);
                credits = pack.getCredits();
                priceCents = (int) pack.getPriceCents();
                planName = pack.getLabel();
                logger.info("[Razorpay] Using CreditPack enum: " + planName);
            }
        } catch (Exception e) {
            // Fallback to CreditPack enum if database lookup fails
            CreditPack pack = CreditPack.fromId(packId);
            credits = pack.getCredits();
            priceCents = (int) pack.getPriceCents();
            planName = pack.getLabel();
            logger.info("[Razorpay] Fallback to CreditPack enum: " + planName);
        }

        try {
            // Create Razorpay order first
            JSONObject orderData = new JSONObject();
            orderData.put("amount", priceCents);
            orderData.put("currency", "INR");
            orderData.put("receipt", "order_" + System.currentTimeMillis());

            JSONObject notes = new JSONObject();
            notes.put("workspaceId", workspaceId);
            notes.put("credits", credits);
            notes.put("pack", packId);
            notes.put("userEmail", userEmail);
            notes.put("paymentMethod", "upi");
            orderData.put("notes", notes);

            JSONObject response = makeApiRequest("POST", "/orders", orderData.toString());
            String orderId = response.getString("id");

            logger.info("[Razorpay] Order created for UPI: " + orderId);

            // Generate UPI string
            double amountInRupees = priceCents / 100.0;
            String upiString = String.format("upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s&tr=%s",
                    upiVpa,
                    upiPayeeName,
                    amountInRupees,
                    "AI Studio Credits - " + planName,
                    orderId);

            // Generate QR code
            String qrCodeBase64 = generateQrCodeBase64(upiString);

            return new UpiQrResponse(
                    upiString,
                    qrCodeBase64,
                    String.valueOf(amountInRupees),
                    "INR",
                    orderId,
                    upiVpa
            );

        } catch (Exception e) {
            logger.severe("[Razorpay] Failed to generate UPI QR: " + e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "UPI QR generation failed: " + e.getMessage()
            );
        }
    }

    private String generateQrCodeBase64(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300, hints);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    @Transactional
    public void processWebhook(String payload, String webhookSignature) {
        try {
            // Generate signature using webhook secret
            String generatedSignature = generateWebhookSignature(payload, razorpayWebhookSecret);
            if (!generatedSignature.equals(webhookSignature)) {
                logger.severe("[Razorpay] Invalid webhook signature received");
                throw new RuntimeException("Invalid webhook signature");
            }

            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");
            logger.info("[Razorpay] Processing webhook event: " + eventType);

            if ("payment.authorized".equals(eventType)) {
                JSONObject paymentData = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                String orderId = paymentData.optString("order_id");
                if (orderId != null && !orderId.isEmpty()) {
                    logger.info("[Razorpay] Payment authorized for order: " + orderId);
                }
            } else if ("payment.failed".equals(eventType)) {
                JSONObject paymentData = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                String orderId = paymentData.optString("order_id");
                logger.warning("[Razorpay] Payment failed for order: " + orderId);
            }
        } catch (Exception e) {
            logger.severe("[Razorpay] Webhook processing failed: " + e.getMessage());
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    private String generateWebhookSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    secret.trim().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.severe("[Razorpay] Failed to generate webhook signature: " + e.getMessage());
            throw new RuntimeException("Webhook signature generation failed", e);
        }
    }
}
