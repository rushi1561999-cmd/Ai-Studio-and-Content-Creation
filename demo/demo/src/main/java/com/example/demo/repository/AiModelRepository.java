package com.example.demo.repository;

import com.example.demo.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiModelRepository extends JpaRepository<AiModel, String> {
    Optional<AiModel> findByModelKey(String modelKey);
    List<AiModel> findByActiveTrueOrderByDisplayNameAsc();
    List<AiModel> findByContentTypeAndActiveTrue(String contentType);
}
