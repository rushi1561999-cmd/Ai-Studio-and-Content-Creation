package com.example.demo.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${ai.gemini.api.key:}")
    private String apiKey;

    @Value("${ai.gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateText(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Error: Set GEMINI_API_KEY or ai.gemini.api.key in application-local.properties.";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String fullUrl = apiUrl + "?key=" + apiKey.trim();

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.postForEntity(fullUrl, request, Map.class);
            return extractText(response.getBody());

        } catch (HttpStatusCodeException e) {
            String detail = extractApiError(e);
            System.err.println("Gemini API error (" + e.getStatusCode() + "): " + detail);
            return "Error: " + detail;
        } catch (Exception e) {
            System.err.println("AI Generation Failed: " + e.getMessage());
            return "Error: Could not generate content. " + e.getMessage();
        }
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
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        return (String) parts.get(0).get("text");
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
}
