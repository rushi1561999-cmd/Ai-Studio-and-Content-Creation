package com.example.demo.queue;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.AiJobMessage;
import com.example.demo.entity.OutboxEvent;
import com.example.demo.repository.OutboxEventRepository;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "ai.processing.rabbitmq", havingValue = "true")
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository repository,
            RabbitTemplate rabbitTemplate,
            JsonMapper objectMapper) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${ai.outbox.publish-delay-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = repository.findPending(
                LocalDateTime.now(),
                PageRequest.of(0, 50));
        for (OutboxEvent event : events) {
            try {
                AiJobMessage message = objectMapper.readValue(event.getPayload(), AiJobMessage.class);
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        event.getRoutingKey(),
                        message);
                event.setPublishedAt(LocalDateTime.now());
                event.setLastError(null);
            } catch (Exception exception) {
                int attempts = event.getAttempts() + 1;
                event.setAttempts(attempts);
                event.setLastError("Publish failed; retry scheduled.");
                event.setAvailableAt(LocalDateTime.now().plusSeconds(Math.min(60, 1L << Math.min(attempts, 6))));
            }
            repository.save(event);
        }
    }
}
