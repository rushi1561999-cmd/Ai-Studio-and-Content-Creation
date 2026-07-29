package com.example.demo.repository;

import com.example.demo.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event from OutboxEvent event
            where event.publishedAt is null and event.availableAt <= :now
            order by event.createdAt
            """)
    List<OutboxEvent> findPending(LocalDateTime now, Pageable pageable);
}
