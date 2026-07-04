package com.example.demo.controller;

import com.example.demo.dto.StripeCheckoutResponse;
import com.example.demo.service.StripeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private final StripeService stripeService;

    public StripeController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("enabled", stripeService.isConfigured()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<StripeCheckoutResponse> checkout(
            @RequestParam String workspaceId,
            @RequestParam String pack) {
        return ResponseEntity.ok(stripeService.createCheckoutSession(workspaceId, pack));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().body("Missing Stripe-Signature header");
        }
        stripeService.handleWebhook(payload, signature);
        return ResponseEntity.ok("ok");
    }
}
