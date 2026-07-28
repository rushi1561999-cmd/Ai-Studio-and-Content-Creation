package com.example.demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.provider", havingValue = "caffeine", matchIfMissing = true)
public class PromptCacheConfig {

    public static final String PROMPT_SUGGESTIONS_CACHE = "promptSuggestions";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                PROMPT_SUGGESTIONS_CACHE,
                "referenceData",
                "dashboardStats");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        return manager;
    }
}
