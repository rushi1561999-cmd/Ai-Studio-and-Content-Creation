package com.example.demo.service.billing;

import com.example.demo.entity.Payment;
import com.example.demo.enums.PaymentProvider;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentFulfillmentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletBillingService walletBillingService;

    @Mock
    private SubscriptionBillingService subscriptionBillingService;

    private PaymentFulfillmentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentFulfillmentService(
                paymentRepository,
                walletBillingService,
                subscriptionBillingService);
    }

    @Test
    void completedPaymentIsIdempotent() {
        Payment payment = payment(PaymentStatus.COMPLETED);
        when(paymentRepository.findByProviderAndExternalIdForUpdate(
                PaymentProvider.RAZORPAY, "order_1"))
                .thenReturn(Optional.of(payment));

        Payment result = service.fulfill(
                PaymentProvider.RAZORPAY,
                "order_1",
                "pay_1",
                9_000,
                "INR");

        assertThat(result).isSameAs(payment);
        verify(walletBillingService, never()).credit(any(), anyInt(), any(), any(), any());
        verify(subscriptionBillingService, never()).activatePaid(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void creditPackCreditsWalletAndCompletesPaymentOnce() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findByProviderAndExternalIdForUpdate(
                PaymentProvider.RAZORPAY, "order_1"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment result = service.fulfill(
                PaymentProvider.RAZORPAY,
                "order_1",
                "pay_1",
                9_000,
                "INR");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getProviderPaymentId()).isEqualTo("pay_1");
        verify(walletBillingService).credit(
                "workspace-1",
                100,
                com.example.demo.enums.CreditTransactionType.PURCHASE,
                "payment-1",
                "RAZORPAY credit purchase");
    }

    @Test
    void subscriptionPaymentActivatesPlanInsteadOfDoubleCreditingWallet() {
        Payment payment = payment(PaymentStatus.PENDING);
        payment.setSubscriptionId("subscription-1");
        when(paymentRepository.findByProviderAndExternalIdForUpdate(
                PaymentProvider.RAZORPAY, "order_1"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        service.fulfill(
                PaymentProvider.RAZORPAY,
                "order_1",
                "pay_1",
                9_000,
                "INR");

        verify(subscriptionBillingService).activatePaid("subscription-1", "payment-1");
        verify(walletBillingService, never()).credit(any(), anyInt(), any(), any(), any());
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId("payment-1");
        payment.setWorkspaceId("workspace-1");
        payment.setProvider(PaymentProvider.RAZORPAY);
        payment.setExternalId("order_1");
        payment.setAmountCents(9_000);
        payment.setCurrency("INR");
        payment.setCreditsGranted(100);
        payment.setStatus(status);
        return payment;
    }
}
