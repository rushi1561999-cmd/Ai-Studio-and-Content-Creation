package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean("promptExecutor")
    public ThreadPoolTaskExecutor promptExecutor(
            @Value("${app.executors.prompt.core-size:8}") int coreSize,
            @Value("${app.executors.prompt.max-size:16}") int maxSize,
            @Value("${app.executors.prompt.queue-capacity:100}") int queueCapacity) {
        return build("prompt-", coreSize, maxSize, queueCapacity);
    }

    @Bean("textGenerationExecutor")
    public ThreadPoolTaskExecutor textGenerationExecutor(
            @Value("${app.executors.text.core-size:8}") int coreSize,
            @Value("${app.executors.text.max-size:16}") int maxSize,
            @Value("${app.executors.text.queue-capacity:200}") int queueCapacity) {
        return build("text-generation-", coreSize, maxSize, queueCapacity);
    }

    @Bean("mediaGenerationExecutor")
    public ThreadPoolTaskExecutor mediaGenerationExecutor(
            @Value("${app.executors.media.core-size:2}") int coreSize,
            @Value("${app.executors.media.max-size:4}") int maxSize,
            @Value("${app.executors.media.queue-capacity:40}") int queueCapacity) {
        return build("media-generation-", coreSize, maxSize, queueCapacity);
    }

    @Bean("emailExecutor")
    public ThreadPoolTaskExecutor emailExecutor(
            @Value("${app.executors.email.core-size:2}") int coreSize,
            @Value("${app.executors.email.max-size:4}") int maxSize,
            @Value("${app.executors.email.queue-capacity:100}") int queueCapacity) {
        return build("email-", coreSize, maxSize, queueCapacity);
    }

    private ThreadPoolTaskExecutor build(
            String prefix,
            int coreSize,
            int maxSize,
            int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
