package com.example.demo.queue;

import com.example.demo.dto.AiJobMessage;
import com.example.demo.entity.GenerationJob;
import com.example.demo.enums.ContentType;
import com.example.demo.repository.GenerationJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiJobProducer {

    private final GenerationJobRepository jobRepository;
    private final OutboxService outboxService;
    private final boolean rabbitMqEnabled;

    public AiJobProducer(
            GenerationJobRepository jobRepository,
            OutboxService outboxService,
            @Value("${ai.processing.rabbitmq:false}") boolean rabbitMqEnabled) {
        this.jobRepository = jobRepository;
        this.outboxService = outboxService;
        this.rabbitMqEnabled = rabbitMqEnabled;
    }

    public GenerationJob submitJob(String promptText, String workspaceId, ContentType contentType, String modelKey) {
        GenerationJob job = new GenerationJob();
        job.setPromptText(promptText);
        job.setStatus("PENDING");
        job.setWorkspaceId(workspaceId);
        job.setContentType(contentType.name());
        job.setModelKey(modelKey);

        GenerationJob savedJob = jobRepository.save(job);

        if (rabbitMqEnabled) {
            AiJobMessage message = new AiJobMessage(
                    savedJob.getId(),
                    promptText,
                    contentType.name(),
                    modelKey
            );
            outboxService.enqueueGeneration(message, contentType);
        }

        return savedJob;
    }
}
