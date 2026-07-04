package com.example.demo.service;

import com.example.demo.entity.AiJob;
import com.example.demo.enums.AiProvider;
import com.example.demo.enums.JobStatus;
import com.example.demo.repository.AiJobRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiJobService {

    private final AiJobRepository repository;

    public AiJobService(AiJobRepository repository) {
        this.repository = repository;
    }

    // 1. Create the job in the database instantly
    public AiJob createJob(String userId, String prompt) {
        AiJob job = new AiJob();
        job.setUserId(userId);
        job.setPrompt(prompt);
        job.setProvider(AiProvider.REPLICATE);
        job.setStatus(JobStatus.PENDING);
        return repository.save(job);
    }

    // 2. Process it in the background so the server doesn't freeze
    @Async
    public void processJob(String jobId, String prompt) {
        AiJob job = repository.findById(jobId).orElseThrow();
        
        job.setStatus(JobStatus.PROCESSING);
        repository.save(job);

        try {
            // TODO: We will add the actual Replicate API call here next!
            // For now, let's simulate a 5-second AI generation delay
            Thread.sleep(5000); 
            
            job.setStatus(JobStatus.COMPLETED);
            job.setResultUrl("https://fake-s3-url.com/generated-image.png");
            repository.save(job);
            
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            repository.save(job);
        }
    }
}
