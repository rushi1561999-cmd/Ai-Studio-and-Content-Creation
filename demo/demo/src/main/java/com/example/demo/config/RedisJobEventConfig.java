package com.example.demo.config;

import com.example.demo.dto.JobStatusEvent;
import com.example.demo.service.job.JobEventService;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "ai.job-stream.redis-enabled", havingValue = "true")
public class RedisJobEventConfig {

    @Bean
    public RedisMessageListenerContainer jobEventListenerContainer(
            RedisConnectionFactory connectionFactory,
            JobEventService jobEventService,
            JsonMapper objectMapper) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                String json = new String(message.getBody(), StandardCharsets.UTF_8);
                jobEventService.publishLocal(objectMapper.readValue(json, JobStatusEvent.class));
            } catch (Exception ignored) {
                // Ignore malformed pub/sub messages; valid job state remains in MySQL.
            }
        }, new ChannelTopic(JobEventService.REDIS_CHANNEL));
        return container;
    }
}
