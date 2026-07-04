package com.example.demo.controller;

import com.example.demo.dto.JobRequest;
import com.example.demo.entity.AiJob;
import com.example.demo.service.AiJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class AiJobController {

    private final AiJobService aiJobService;

    public AiJobController(AiJobService aiJobService) {
        this.aiJobService = aiJobService;
    }

    @PostMapping("/generate")
    public ResponseEntity<AiJob> generateContent(@RequestBody JobRequest request) {
        // Hardcoded user for now, later this comes from your JWT Auth login
        String userId = "user-123"; 

        // 1. Save the pending job to MySQL
        AiJob job = aiJobService.createJob(userId, request.getPrompt());

        // 2. Start the background generation task
        aiJobService.processJob(job.getId(), request.getPrompt());

        // 3. Return the pending job back to React immediately!
        return ResponseEntity.ok(job);
    }
}
