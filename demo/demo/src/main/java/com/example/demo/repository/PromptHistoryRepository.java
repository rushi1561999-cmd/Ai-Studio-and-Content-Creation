package com.example.demo.repository;

import com.example.demo.entity.PromptHistory;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PromptHistoryRepository extends JpaRepository<PromptHistory, String> {
    void deleteByUser_Id(String userId);
}
