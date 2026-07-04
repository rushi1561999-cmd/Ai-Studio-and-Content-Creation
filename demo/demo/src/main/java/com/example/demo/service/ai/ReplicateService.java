package com.example.demo.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class ReplicateService {

    private static final String FLUX_PREDICTIONS =
            "https://api.replicate.com/v1/models/black-forest-labs/flux-schnell/predictions";
    private static final String VIDEO_PREDICTIONS =
            "https://api.replicate.com/v1/models/anotherjesse/zeroscope-v2-xl/predictions";

    @Value("${replicate.api.token:}")
    private String apiToken;

    @Value("${ai.image.fallback.enabled:true}")
    private boolean imageFallbackEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return apiToken != null && !apiToken.isBlank();
    }

    public GenerationResult generateImage(String prompt) {
        if (isConfigured()) {
            try {
                String url = runModelPrediction(FLUX_PREDICTIONS, Map.of(
                        "prompt", prompt,
                        "num_outputs", 1
                ));
                if (url != null) {
                    return GenerationResult.okMedia(url);
                }
            } catch (Exception e) {
                System.err.println("Replicate image failed: " + e.getMessage());
            }
        }

        if (imageFallbackEnabled) {
            return GenerationResult.okMedia(buildPollinationsUrl(prompt));
        }

        return GenerationResult.fail(
                "Image generation requires replicate.api.token or enable ai.image.fallback.enabled."
        );
    }

    public GenerationResult generateVideo(String prompt) {
        if (!isConfigured()) {
            return GenerationResult.fail(
                    "Video generation requires replicate.api.token in application.properties."
            );
        }

        try {
            String url = runModelPrediction(VIDEO_PREDICTIONS, Map.of("prompt", prompt));
            if (url != null) {
                return GenerationResult.okMedia(url);
            }
            return GenerationResult.fail("Video generation returned no output URL.");
        } catch (Exception e) {
            return GenerationResult.fail("Video generation failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String runModelPrediction(String endpoint, Map<String, Object> input) throws InterruptedException {
        HttpHeaders headers = authHeaders();
        Map<String, Object> body = Map.of("input", input);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> createRes = restTemplate.postForEntity(endpoint, request, Map.class);
        Map<String, Object> created = createRes.getBody();
        if (created == null) {
            return null;
        }

        String pollUrl = created.get("urls") instanceof Map<?, ?> urls
                ? String.valueOf(((Map<String, Object>) urls).get("get"))
                : null;
        if (pollUrl == null || "null".equals(pollUrl)) {
            pollUrl = created.get("id") != null
                    ? "https://api.replicate.com/v1/predictions/" + created.get("id")
                    : null;
        }

        if (pollUrl == null) {
            return null;
        }

        for (int i = 0; i < 90; i++) {
            Thread.sleep(2000);
                ResponseEntity<Map> pollRes = restTemplate.exchange(
                    pollUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> poll = pollRes.getBody();
            if (poll == null) {
                continue;
            }

            String status = String.valueOf(poll.get("status"));
            if ("succeeded".equals(status)) {
                return extractOutputUrl(poll.get("output"));
            }
            if ("failed".equals(status) || "canceled".equals(status)) {
                Object error = poll.get("error");
                throw new IllegalStateException(error != null ? error.toString() : status);
            }
        }
        throw new IllegalStateException("Timed out waiting for Replicate prediction.");
    }

    @SuppressWarnings("unchecked")
    private String extractOutputUrl(Object output) {
        if (output == null) {
            return null;
        }
        if (output instanceof String s) {
            return s;
        }
        if (output instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first != null ? first.toString() : null;
        }
        return output.toString();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken.trim());
        return headers;
    }

    private String buildPollinationsUrl(String prompt) {
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
        return "https://image.pollinations.ai/prompt/" + encoded + "?width=1024&height=1024&nologo=true";
    }
}
