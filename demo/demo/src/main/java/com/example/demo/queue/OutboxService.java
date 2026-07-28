package com.example.demo.queue;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.AiJobMessage;
import com.example.demo.entity.OutboxEvent;
import com.example.demo.enums.ContentType;
import com.example.demo.repository.OutboxEventRepository;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final JsonMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, JsonMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void enqueueGeneration(AiJobMessage message, ContentType contentType) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(message.getJobId());
        event.setEventType("GENERATION_REQUESTED");
        event.setRoutingKey(routingKey(contentType));
        try {
            event.setPayload(objectMapper.writeValueAsString(message));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize generation event.", exception);
        }
        repository.save(event);
    }

    private String routingKey(ContentType contentType) {
        return switch (contentType) {
            case TEXT -> RabbitMQConfig.TEXT_ROUTING_KEY;
            case VIDEO -> RabbitMQConfig.VIDEO_ROUTING_KEY;
            case IMAGE, MIXED -> RabbitMQConfig.IMAGE_ROUTING_KEY;
        };
    }
}
