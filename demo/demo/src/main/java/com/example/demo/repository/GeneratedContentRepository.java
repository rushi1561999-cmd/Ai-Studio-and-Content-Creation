package com.example.demo.repository;

import com.example.demo.entity.GeneratedContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, String> {
    Optional<GeneratedContent> findByGenerationJobId(String generationJobId);
    Page<GeneratedContent> findByWorkspaceId(String workspaceId, Pageable pageable);
}
