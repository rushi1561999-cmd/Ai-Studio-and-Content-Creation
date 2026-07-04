package com.example.demo.repository;

import com.example.demo.entity.SavedPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedPromptRepository extends JpaRepository<SavedPrompt, String> {
    List<SavedPrompt> findByUser_IdOrderBySavedAtDesc(String userId);
    boolean existsByUser_IdAndPost_Id(String userId, String postId);
    void deleteByPost_Id(String postId);

    void deleteByUser_Id(String userId);
}
