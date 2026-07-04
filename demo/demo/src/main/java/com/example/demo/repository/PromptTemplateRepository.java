package com.example.demo.repository;

import com.example.demo.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, String> {
    List<PromptTemplate> findByCategory_IdOrderByTitleAsc(String categoryId);
}
