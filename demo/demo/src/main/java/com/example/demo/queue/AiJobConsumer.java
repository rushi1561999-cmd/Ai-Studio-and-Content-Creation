package com.example.demo.queue;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.AiJobMessage;
import com.example.demo.service.job.GenerationJobService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "ai.processing.rabbitmq", havingValue = "true")
public class AiJobConsumer {

    private final GenerationJobService generationJobService;

    public AiJobConsumer(GenerationJobService generationJobService) {
        this.generationJobService = generationJobService;
    }

    @RabbitListener(
            queues = RabbitMQConfig.TEXT_QUEUE,
            concurrency = "${ai.workers.text.concurrency:8-16}")
    public void processTextJob(AiJobMessage message) {
        generationJobService.process(message.getJobId());
    }

    @RabbitListener(
            queues = RabbitMQConfig.IMAGE_QUEUE,
            concurrency = "${ai.workers.image.concurrency:2-4}")
    public void processImageJob(AiJobMessage message) {
        generationJobService.process(message.getJobId());
    }

    @RabbitListener(
            queues = RabbitMQConfig.VIDEO_QUEUE,
            concurrency = "${ai.workers.video.concurrency:1-2}")
    public void processVideoJob(AiJobMessage message) {
        generationJobService.process(message.getJobId());
    }
}
