package com.example.demo.repository;

import com.example.demo.entity.StripePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StripePaymentRepository extends JpaRepository<StripePayment, String> {
}
