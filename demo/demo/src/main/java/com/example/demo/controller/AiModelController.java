package com.example.demo.controller;

import com.example.demo.entity.AiModel;
import com.example.demo.repository.AiModelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-models")
public class AiModelController {

    private final AiModelRepository aiModelRepository;

    public AiModelController(AiModelRepository aiModelRepository) {
        this.aiModelRepository = aiModelRepository;
    }

    @GetMapping
    public ResponseEntity<List<AiModel>> listActive(
            @RequestParam(required = false) String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return ResponseEntity.ok(
                    aiModelRepository.findByContentTypeAndActiveTrue(contentType.trim().toUpperCase()));
        }
        return ResponseEntity.ok(aiModelRepository.findByActiveTrueOrderByDisplayNameAsc());
    }
}
