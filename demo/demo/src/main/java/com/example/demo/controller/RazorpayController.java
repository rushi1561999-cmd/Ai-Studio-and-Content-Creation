package com.example.demo.controller;

import com.example.demo.dto.RazorpayCheckoutResponse;
import com.example.demo.dto.RazorpayVerifyRequest;
import com.example.demo.entity.Payment;
import com.example.demo.service.RazorpayService;
import com.example.demo.util.SecurityUtils;
import jakarta.validation.Valid;
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
            "enabled", razorpayService.isConfigured()
        ));
    }

    @PostMapping("/order")
    public ResponseEntity<RazorpayCheckoutResponse> createOrder(
            @RequestParam String workspaceId,
            @RequestParam(required = false) String pack,
            @RequestParam(required = false) String subscriptionId) {
        String userEmail = SecurityUtils.currentUserEmail();
        return ResponseEntity.ok(razorpayService.createOrder(workspaceId, pack, subscriptionId, userEmail));
    }

    @PostMapping("/verify")
    public ResponseEntity<Payment> verifyPayment(@Valid @RequestBody RazorpayVerifyRequest request) {
        return ResponseEntity.ok(razorpayService.verifyPayment(
                request.getOrderId(),
                request.getPaymentId(),
                request.getSignature()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String webhookSignature) {
        if (webhookSignature == null || webhookSignature.isBlank()) {
            return ResponseEntity.badRequest().body("Missing X-Razorpay-Signature header");
        }
        razorpayService.processWebhook(payload, webhookSignature);
        return ResponseEntity.ok("Webhook processed");
    }
}
