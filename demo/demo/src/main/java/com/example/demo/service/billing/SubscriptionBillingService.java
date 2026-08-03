package com.example.demo.service.billing;

import com.example.demo.entity.Subscription;
import com.example.demo.entity.SubscriptionPlan;
import com.example.demo.enums.CreditTransactionType;
import com.example.demo.repository.SubscriptionPlanRepository;
import com.example.demo.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class SubscriptionBillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final WalletBillingService walletBillingService;

    public SubscriptionBillingService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            WalletBillingService walletBillingService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.walletBillingService = walletBillingService;
    }

    @Transactional
    public Subscription createPending(String workspaceId, String planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active plan not found."));
        if (plan.getPriceCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This plan does not require checkout.");
        }
        if (subscriptionRepository.findByWorkspaceIdAndStatus(workspaceId, "ACTIVE").isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancel the current plan before choosing another one.");
        }

        var pendingSubscriptions = subscriptionRepository.findAllByWorkspaceIdAndStatus(workspaceId, "PENDING");
        var reusable = pendingSubscriptions.stream()
                .filter(pending -> plan.getId().equals(pending.getPlanId()))
                .findFirst();
        if (reusable.isPresent()) {
            return reusable.get();
        }
        pendingSubscriptions.forEach(pending -> {
                    pending.setStatus("CANCELLED");
                    subscriptionRepository.save(pending);
                });

        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspaceId);
        subscription.setPlanId(plan.getId());
        subscription.setPlanName(plan.getName());
        subscription.setMonthlyCredits(plan.getMonthlyCredits());
        subscription.setStatus("PENDING");
        subscription.setRenewalDate(null);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription activatePaid(String subscriptionId, String paymentReference) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found."));
        if ("ACTIVE".equals(subscription.getStatus())) {
            LocalDateTime extensionStart = subscription.getRenewalDate() != null
                    && subscription.getRenewalDate().isAfter(LocalDateTime.now())
                    ? subscription.getRenewalDate()
                    : LocalDateTime.now();
            subscription.setRenewalDate(extensionStart.plusDays(30));
            Subscription extended = subscriptionRepository.save(subscription);
            walletBillingService.credit(
                    subscription.getWorkspaceId(),
                    subscription.getMonthlyCredits(),
                    CreditTransactionType.SUBSCRIPTION,
                    paymentReference,
                    subscription.getPlanName() + " additional plan period credits");
            return extended;
        }
        if (!"PENDING".equals(subscription.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subscription is not awaiting payment.");
        }

        subscriptionRepository.findByWorkspaceIdAndStatus(subscription.getWorkspaceId(), "ACTIVE")
                .filter(active -> !active.getId().equals(subscriptionId))
                .ifPresent(active -> {
                    active.setStatus("CANCELLED");
                    active.setRenewalDate(null);
                    subscriptionRepository.save(active);
                });

        subscription.setStatus("ACTIVE");
        subscription.setRenewalDate(LocalDateTime.now().plusDays(30));
        Subscription saved = subscriptionRepository.save(subscription);

        walletBillingService.credit(
                subscription.getWorkspaceId(),
                subscription.getMonthlyCredits(),
                CreditTransactionType.SUBSCRIPTION,
                paymentReference,
                subscription.getPlanName() + " plan credits");
        return saved;
    }

    @Transactional
    public void cancel(String workspaceId, String subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found."));
        if (!workspaceId.equals(subscription.getWorkspaceId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Subscription does not belong to this workspace.");
        }
        if (!"ACTIVE".equals(subscription.getStatus()) && !"PENDING".equals(subscription.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subscription is already closed.");
        }
        subscription.setStatus("CANCELLED");
        subscription.setRenewalDate(null);
        subscriptionRepository.save(subscription);
    }
}
