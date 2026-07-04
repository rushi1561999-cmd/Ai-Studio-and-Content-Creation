package com.example.demo.controller;

import com.example.demo.entity.CreditTransaction;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Subscription;
import com.example.demo.entity.SubscriptionPlan;
import com.example.demo.repository.CreditTransactionRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SubscriptionRepository;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.billing.SubscriptionPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public BillingController(
            SubscriptionPlanService subscriptionPlanService,
            CreditTransactionRepository creditTransactionRepository,
            PaymentRepository paymentRepository,
            SubscriptionRepository subscriptionRepository,
            WorkspaceAccessService workspaceAccessService) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.creditTransactionRepository = creditTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> listPlans() {
        return ResponseEntity.ok(subscriptionPlanService.listActivePlans());
    }

    @GetMapping("/workspace/{workspaceId}/transactions")
    public ResponseEntity<List<CreditTransaction>> transactions(@PathVariable String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return ResponseEntity.ok(creditTransactionRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId));
    }

    @GetMapping("/workspace/{workspaceId}/payments")
    public ResponseEntity<List<Payment>> payments(@PathVariable String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return ResponseEntity.ok(paymentRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId));
    }

    @GetMapping("/workspace/{workspaceId}/payment-methods")
    public ResponseEntity<List<Map<String, String>>> paymentMethods(@PathVariable String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        // Return empty list for now - payment methods not implemented
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/workspace/{workspaceId}/subscription")
    public ResponseEntity<Subscription> subscription(@PathVariable String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return subscriptionRepository.findByWorkspaceIdAndStatus(workspaceId, "ACTIVE")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(null));
    }

    @PostMapping("/workspace/{workspaceId}/subscribe")
    public ResponseEntity<Subscription> subscribe(
            @PathVariable String workspaceId,
            @RequestBody Map<String, String> request) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        
        String planId = request.get("planId");
        if (planId == null) {
            return ResponseEntity.badRequest().build();
        }

        SubscriptionPlan plan = subscriptionPlanService.findById(planId);
        if (plan == null) {
            return ResponseEntity.badRequest().build();
        }

        // Cancel any existing subscription
        subscriptionRepository.findByWorkspaceIdAndStatus(workspaceId, "ACTIVE")
                .ifPresent(sub -> {
                    sub.setStatus("CANCELLED");
                    subscriptionRepository.save(sub);
                });

        // Create new subscription
        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspaceId);
        subscription.setPlanId(planId);
        subscription.setPlanName(plan.getName());
        subscription.setMonthlyCredits(plan.getMonthlyCredits());
        subscription.setStatus("ACTIVE");
        subscription.setRenewalDate(java.time.LocalDateTime.now().plusMonths(1));
        
        Subscription saved = subscriptionRepository.save(subscription);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/workspace/{workspaceId}/subscription/{subscriptionId}/cancel")
    public ResponseEntity<Void> cancelSubscription(
            @PathVariable String workspaceId,
            @PathVariable String subscriptionId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        
        subscriptionRepository.findById(subscriptionId).ifPresent(sub -> {
            sub.setStatus("CANCELLED");
            subscriptionRepository.save(sub);
        });
        
        return ResponseEntity.noContent().build();
    }
}
