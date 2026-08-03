package com.example.demo.repository;

import com.example.demo.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    Optional<Subscription> findByWorkspaceIdAndStatus(String workspaceId, String status);
    List<Subscription> findAllByWorkspaceIdAndStatus(String workspaceId, String status);
    boolean existsByPlanId(String planId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select subscription from Subscription subscription where subscription.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") String id);
}
