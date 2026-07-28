package com.example.demo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.provider", havingValue = "redis")
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration fiveMinutes = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(fiveMinutes)
                .withCacheConfiguration(
                        "referenceData",
                        fiveMinutes.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(
                        "dashboardStats",
                        fiveMinutes.entryTtl(Duration.ofSeconds(30)))
                .build();
    }
}
