package com.example.demo.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class GeminiService {

    private static final int CIRCUIT_FAILURE_THRESHOLD = 3;
    private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(30);

    @Value("${ai.gemini.api.key:}")
    private String apiKey;

    @Value("${ai.gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicReference<Instant> circuitOpenUntil = new AtomicReference<>();

    public GeminiService(RestTemplate providerRestTemplate) {
        this.restTemplate = providerRestTemplate;
    }

    public String generateText(String prompt) {
        return generate(prompt, false);
    }

    public String generateJson(String prompt) {
        return generate(prompt, true);
    }

    private String generate(String prompt, boolean jsonResponse) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Error: Set GEMINI_API_KEY or ai.gemini.api.key in application-local.properties.";
        }

        Instant blockedUntil = circuitOpenUntil.get();
        if (blockedUntil != null && blockedUntil.isAfter(Instant.now())) {
            return "Error: Gemini is temporarily unavailable.";
        }

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Client-Request-Id", UUID.randomUUID().toString());

                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))));
                if (jsonResponse) {
                    requestBody.put("generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "temperature", 0.35,
                            "maxOutputTokens", 2_048));
                }

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                String fullUrl = apiUrl + "?key=" + apiKey.trim();

                @SuppressWarnings("unchecked")
                ResponseEntity<Map<String, Object>> response =
                        (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>)
                                restTemplate.postForEntity(fullUrl, request, Map.class);
                consecutiveFailures.set(0);
                circuitOpenUntil.set(null);
                return extractText(response.getBody());
            } catch (HttpStatusCodeException exception) {
                boolean temporary = exception.getStatusCode().value() == 429
                        || exception.getStatusCode().is5xxServerError();
                if (temporary && attempt == 1) {
                    pauseBeforeRetry();
                    continue;
                }
                recordFailure();
                return "Error: " + extractApiError(exception);
            } catch (ResourceAccessException exception) {
                recordFailure();
                return "Error: Gemini request timed out.";
            } catch (Exception exception) {
                recordFailure();
                return "Error: Could not generate content.";
            }
        }
        recordFailure();
        return "Error: Gemini is temporarily unavailable.";
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> body) {
        if (body == null) {
            return "Error: Empty response from Gemini API.";
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            Map<String, Object> feedback = (Map<String, Object>) body.get("promptFeedback");
            if (feedback != null && feedback.get("blockReason") != null) {
                return "Error: Prompt blocked — " + feedback.get("blockReason");
            }
            return "Error: No content returned from Gemini API.";
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            return "Error: Invalid response from Gemini API.";
        }
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty() || parts.get(0).get("text") == null) {
            return "Error: No text returned from Gemini API.";
        }
        return String.valueOf(parts.get(0).get("text"));
    }

    private String extractApiError(HttpStatusCodeException e) {
        try {
            Map<?, ?> body = e.getResponseBodyAs(Map.class);
            if (body != null && body.get("error") instanceof Map<?, ?> error) {
                Object message = error.get("message");
                if (message != null) {
                    return message.toString();
                }
            }
        } catch (Exception ignored) {
            // fall through to default message
        }
        if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
            return "Invalid or unauthorized Gemini API key.";
        }
        if (e.getStatusCode().value() == 404) {
            return "Gemini model not found. Check ai.gemini.api.url in application.properties.";
        }
        return "Gemini request failed (" + e.getStatusCode() + ").";
    }

    private void recordFailure() {
        if (consecutiveFailures.incrementAndGet() >= CIRCUIT_FAILURE_THRESHOLD) {
            circuitOpenUntil.set(Instant.now().plus(CIRCUIT_OPEN_DURATION));
            consecutiveFailures.set(0);
        }
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
