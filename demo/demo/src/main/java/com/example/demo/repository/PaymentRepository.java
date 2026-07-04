package com.example.demo.repository;

import com.example.demo.entity.Payment;
import com.example.demo.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByProviderAndExternalId(PaymentProvider provider, String externalId);
    List<Payment> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
}
