package com.example.demo.repository;

import com.example.demo.entity.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, String> {
    // This allows us to easily fetch all prompts for a specific workspace!
    List<Prompt> findByWorkspaceId(String workspaceId);

    void deleteByCreatedBy_Id(String userId);

    java.util.List<Prompt> findByCreatedBy_Id(String userId);
}
