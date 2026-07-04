package com.example.demo.repository;

import com.example.demo.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> {
    Optional<SubscriptionPlan> findByCode(String code);
    List<SubscriptionPlan> findByActiveTrueOrderByPriceCentsAsc();
}
