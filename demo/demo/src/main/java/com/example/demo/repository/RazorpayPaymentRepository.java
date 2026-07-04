package com.example.demo.repository;

import com.example.demo.entity.RazorpayPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RazorpayPaymentRepository extends JpaRepository<RazorpayPayment, String> {
    Optional<RazorpayPayment> findByPaymentId(String paymentId);
    Optional<RazorpayPayment> findByOrderId(String orderId);
}
