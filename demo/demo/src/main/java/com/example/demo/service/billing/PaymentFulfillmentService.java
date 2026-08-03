package com.example.demo.service.billing;

import com.example.demo.entity.Payment;
import com.example.demo.enums.CreditTransactionType;
import com.example.demo.enums.PaymentProvider;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentFulfillmentService {

    private final PaymentRepository paymentRepository;
    private final WalletBillingService walletBillingService;
    private final SubscriptionBillingService subscriptionBillingService;

    public PaymentFulfillmentService(
            PaymentRepository paymentRepository,
            WalletBillingService walletBillingService,
            SubscriptionBillingService subscriptionBillingService) {
        this.paymentRepository = paymentRepository;
        this.walletBillingService = walletBillingService;
        this.subscriptionBillingService = subscriptionBillingService;
    }

    @Transactional
    public Payment fulfill(
            PaymentProvider provider,
            String externalId,
            String providerPaymentId,
            int verifiedAmount,
            String verifiedCurrency) {
        Payment payment = paymentRepository.findByProviderAndExternalIdForUpdate(provider, externalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found."));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return payment;
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment has already been refunded.");
        }
        if (payment.getAmountCents() != verifiedAmount
                || !payment.getCurrency().equalsIgnoreCase(verifiedCurrency)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Provider amount does not match the order.");
        }

        if (payment.getSubscriptionId() != null && !payment.getSubscriptionId().isBlank()) {
            subscriptionBillingService.activatePaid(payment.getSubscriptionId(), payment.getId());
        } else {
            walletBillingService.credit(
                    payment.getWorkspaceId(),
                    payment.getCreditsGranted(),
                    CreditTransactionType.PURCHASE,
                    payment.getId(),
                    provider.name() + " credit purchase");
        }

        payment.setProviderPaymentId(providerPaymentId);
        payment.setStatus(PaymentStatus.COMPLETED);
        return paymentRepository.save(payment);
    }

    @Transactional
    public void markFailed(PaymentProvider provider, String externalId, String providerPaymentId) {
        paymentRepository.findByProviderAndExternalIdForUpdate(provider, externalId)
                .ifPresent(payment -> {
                    if (payment.getStatus() != PaymentStatus.COMPLETED) {
                        payment.setProviderPaymentId(providerPaymentId);
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                    }
                });
    }
}
