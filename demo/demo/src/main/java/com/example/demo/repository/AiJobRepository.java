package com.example.demo.repository;



import com.example.demo.entity.AiJob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiJobRepository extends JpaRepository<AiJob, String> {
    
    // Spring Boot magically writes the SQL query for this just by reading the method name!
    // Equivalent to: SELECT * FROM ai_jobs WHERE provider_job_id = ?
    Optional<AiJob> findByProviderJobId(String providerJobId);

    void deleteByUserId(String userId);
}