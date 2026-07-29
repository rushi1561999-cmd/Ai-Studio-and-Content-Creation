package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ProviderHttpConfig {

    @Bean
    public RestTemplate providerRestTemplate(
            RestTemplateBuilder builder,
            @Value("${ai.provider.connect-timeout:2s}") Duration connectTimeout,
            @Value("${ai.provider.read-timeout:5s}") Duration readTimeout) {
        return builder
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .build();
    }
}
