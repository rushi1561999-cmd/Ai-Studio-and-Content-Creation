package com.example.demo.service;

import com.example.demo.service.job.GenerationJobService;
import org.springframework.stereotype.Service;

@Service
public class GenerationJobProcessor {

    private final GenerationJobService generationJobService;

    public GenerationJobProcessor(GenerationJobService generationJobService) {
        this.generationJobService = generationJobService;
    }

    public void processAsync(String jobId, String promptText) {
        generationJobService.processAsync(jobId);
    }
}
