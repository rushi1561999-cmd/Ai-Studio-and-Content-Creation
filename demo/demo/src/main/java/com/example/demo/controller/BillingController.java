package com.example.demo.controller;

import com.example.demo.entity.CreditTransaction;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Subscription;
import com.example.demo.entity.SubscriptionPlan;
import com.example.demo.dto.SubscribeRequest;
import com.example.demo.repository.CreditTransactionRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.SubscriptionRepository;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.billing.SubscriptionBillingService;
import com.example.demo.service.billing.SubscriptionPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final SubscriptionBillingService subscriptionBillingService;

    public BillingController(
            SubscriptionPlanService subscriptionPlanService,
            CreditTransactionRepository creditTransactionRepository,
            PaymentRepository paymentRepository,
            SubscriptionRepository subscriptionRepository,
            WorkspaceAccessService workspaceAccessService,
            SubscriptionBillingService subscriptionBillingService) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.creditTransactionRepository = creditTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.subscriptionBillingService = subscriptionBillingService;
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
            @Valid @RequestBody SubscribeRequest request) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return ResponseEntity.ok(subscriptionBillingService.createPending(workspaceId, request.getPlanId()));
    }

    @PostMapping("/workspace/{workspaceId}/subscription/{subscriptionId}/cancel")
    public ResponseEntity<Void> cancelSubscription(
            @PathVariable String workspaceId,
            @PathVariable String subscriptionId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        
        subscriptionBillingService.cancel(workspaceId, subscriptionId);
        return ResponseEntity.noContent().build();
    }
}
