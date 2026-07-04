package com.example.demo.queue;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.AiJobMessage;
import com.example.demo.service.GenerationJobProcessor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "ai.processing.rabbitmq", havingValue = "true")
public class AiJobConsumer {

    private final GenerationJobProcessor jobProcessor;

    public AiJobConsumer(GenerationJobProcessor jobProcessor) {
        this.jobProcessor = jobProcessor;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void processAiJob(AiJobMessage message) {
        jobProcessor.processAsync(message.getJobId(), message.getPromptText());
    }
}