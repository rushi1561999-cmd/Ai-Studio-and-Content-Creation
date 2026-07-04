package com.example.demo.controller;

import com.example.demo.dto.RazorpayCheckoutResponse;
import com.example.demo.dto.UpiQrResponse;
import com.example.demo.service.RazorpayService;
import com.example.demo.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/razorpay")
public class RazorpayController {

    private final RazorpayService razorpayService;

    public RazorpayController(RazorpayService razorpayService) {
        this.razorpayService = razorpayService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of(
            "enabled", razorpayService.isConfigured(),
            "upiEnabled", razorpayService.isUpiConfigured()
        ));
    }

    @PostMapping("/order")
    public ResponseEntity<RazorpayCheckoutResponse> createOrder(
            @RequestParam String workspaceId,
            @RequestParam String pack) {
        String userEmail = SecurityUtils.currentUserEmail();
        return ResponseEntity.ok(razorpayService.createOrder(workspaceId, pack, userEmail));
    }

    @PostMapping("/upi/qr")
    public ResponseEntity<UpiQrResponse> generateUpiQr(
            @RequestParam String workspaceId,
            @RequestParam String pack) {
        String userEmail = SecurityUtils.currentUserEmail();
        return ResponseEntity.ok(razorpayService.generateUpiQrCode(workspaceId, pack, userEmail));
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestParam String signature) {
        razorpayService.verifyPayment(orderId, paymentId, signature);
        return ResponseEntity.ok("Payment verified successfully");
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String webhookSignature) {
        razorpayService.processWebhook(payload, webhookSignature);
        return ResponseEntity.ok("Webhook processed");
    }
}
