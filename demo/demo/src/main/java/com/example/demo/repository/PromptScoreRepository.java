package com.example.demo.repository;

import com.example.demo.entity.PromptScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromptScoreRepository extends JpaRepository<PromptScore, String> {
    List<PromptScore> findByPrompt_Id(String promptId);
}
