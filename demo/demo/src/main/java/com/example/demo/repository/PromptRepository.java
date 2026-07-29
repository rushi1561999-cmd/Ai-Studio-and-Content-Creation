package com.example.demo.repository;

import com.example.demo.entity.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, String> {
    Page<Prompt> findByWorkspace_Id(String workspaceId, Pageable pageable);

    void deleteByCreatedBy_Id(String userId);

    java.util.List<Prompt> findByCreatedBy_Id(String userId);
}
