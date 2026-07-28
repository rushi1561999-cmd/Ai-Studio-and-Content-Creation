package com.example.demo.repository;

import com.example.demo.entity.GenerationJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface GenerationJobRepository extends JpaRepository<GenerationJob, String> {

    Page<GenerationJob> findByWorkspaceId(String workspaceId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            update GenerationJob job set job.status = 'PROCESSING'
            where job.id = :jobId and job.status = 'PENDING'
            """)
    int claimPending(String jobId);
}
