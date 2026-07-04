package com.example.demo.repository;

import com.example.demo.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    Optional<Subscription> findByWorkspaceIdAndStatus(String workspaceId, String status);
}
