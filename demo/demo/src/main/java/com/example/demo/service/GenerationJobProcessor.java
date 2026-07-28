package com.example.demo.service;

import com.example.demo.entity.GenerationJob;
import com.example.demo.enums.ContentType;
import com.example.demo.repository.GenerationJobRepository;
import com.example.demo.service.job.GenerationJobService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class GenerationJobProcessor {

    private final GenerationJobService generationJobService;
    private final GenerationJobRepository jobRepository;
    private final ThreadPoolTaskExecutor textExecutor;
    private final ThreadPoolTaskExecutor mediaExecutor;

    public GenerationJobProcessor(
            GenerationJobService generationJobService,
            GenerationJobRepository jobRepository,
            @Qualifier("textGenerationExecutor") ThreadPoolTaskExecutor textExecutor,
            @Qualifier("mediaGenerationExecutor") ThreadPoolTaskExecutor mediaExecutor) {
        this.generationJobService = generationJobService;
        this.jobRepository = jobRepository;
        this.textExecutor = textExecutor;
        this.mediaExecutor = mediaExecutor;
    }

    public void processAsync(String jobId, String promptText) {
        GenerationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        ContentType contentType = ContentType.fromString(job.getContentType());
        ThreadPoolTaskExecutor executor =
                contentType == ContentType.TEXT ? textExecutor : mediaExecutor;
        executor.execute(() -> generationJobService.process(jobId));
    }
}
