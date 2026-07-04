package com.example.demo.service.billing;

import com.example.demo.entity.SubscriptionPlan;
import com.example.demo.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanService(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> listActivePlans() {
        return subscriptionPlanRepository.findByActiveTrueOrderByPriceCentsAsc();
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan findById(String id) {
        return subscriptionPlanRepository.findById(id).orElse(null);
    }
}
