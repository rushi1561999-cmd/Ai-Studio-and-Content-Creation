package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    // Pulls your key from application.properties
    @Value("${ai.gemini.api.key}")
    private String apiKey;

    // Pulls your URL from application.properties
    @Value("${ai.gemini.api.url}")
    private String apiUrl;

    @SuppressWarnings("unchecked")
    public String generateResponse(String promptText) {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. Build the specific nested JSON structure that Gemini requires
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", promptText)
                ))
            )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // Construct the full URL using the injected apiUrl and apiKey
        String fullUrl = apiUrl + "?key=" + apiKey;
        
        try {
            // 2. Send the POST request to Google's servers
            Map<String, Object> response = restTemplate.postForObject(fullUrl, request, Map.class);
            
            // 3. Dig through the response JSON to find the actual text string
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            
            return (String) parts.get(0).get("text");
            
        } catch (Exception e) {
            return "Error connecting to AI: " + e.getMessage();
        }
    }
}
