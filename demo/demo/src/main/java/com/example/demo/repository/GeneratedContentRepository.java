package com.example.demo.repository;

import com.example.demo.entity.GeneratedContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, String> {
    Optional<GeneratedContent> findByGenerationJobId(String generationJobId);
    List<GeneratedContent> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
}
