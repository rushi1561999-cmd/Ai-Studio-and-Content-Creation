package com.example.demo.repository;

import com.example.demo.entity.GenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenerationJobRepository extends JpaRepository<GenerationJob, String> {

    List<GenerationJob> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
}
