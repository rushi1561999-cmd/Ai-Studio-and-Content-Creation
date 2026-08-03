package com.example.demo.repository;

import com.example.demo.entity.Payment;
import com.example.demo.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByProviderAndExternalId(PaymentProvider provider, String externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.provider = :provider and payment.externalId = :externalId")
    Optional<Payment> findByProviderAndExternalIdForUpdate(
            @Param("provider") PaymentProvider provider,
            @Param("externalId") String externalId);
    List<Payment> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<Payment> findByWorkspaceIdAndSubscriptionId(String workspaceId, String subscriptionId);
}
