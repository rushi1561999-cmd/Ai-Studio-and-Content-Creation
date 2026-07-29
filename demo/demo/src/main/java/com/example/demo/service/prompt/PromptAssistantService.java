package com.example.demo.service.prompt;

import com.example.demo.config.PromptCacheConfig;
import com.example.demo.dto.PromptSuggestionRequest;
import com.example.demo.dto.PromptSuggestionResponse;
import com.example.demo.dto.PromptVariantResponse;
import com.example.demo.entity.Subscription;
import com.example.demo.repository.SubscriptionRepository;
import com.example.demo.service.ai.GeminiService;
import com.example.demo.service.prompt.PromptQualityService.PromptQuality;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class PromptAssistantService {

    private final GeminiService geminiService;
    private final PromptQualityService promptQualityService;
    private final SubscriptionRepository subscriptionRepository;
    private final JsonMapper objectMapper;
    private final ThreadPoolTaskExecutor promptExecutor;
    private final int minuteLimit;
    private final int freeDailyLimit;
    private final int paidDailyLimit;
    private final int enterpriseDailyLimit;
    private final int enterpriseCreditThreshold;
    private final Duration timeout;
    private final StringRedisTemplate redisTemplate;
    private final boolean redisRateLimitEnabled;

    private final Map<String, MinuteWindow> minuteWindows = new ConcurrentHashMap<>();
    private final Map<String, DailyWindow> dailyWindows = new ConcurrentHashMap<>();

    public PromptAssistantService(
            GeminiService geminiService,
            PromptQualityService promptQualityService,
            SubscriptionRepository subscriptionRepository,
            JsonMapper objectMapper,
            @Qualifier("promptExecutor") ThreadPoolTaskExecutor promptExecutor,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            @Value("${prompt.assistant.rate-limit-per-minute:10}") int minuteLimit,
            @Value("${prompt.assistant.daily-limit.free:20}") int freeDailyLimit,
            @Value("${prompt.assistant.daily-limit.paid:200}") int paidDailyLimit,
            @Value("${prompt.assistant.daily-limit.enterprise:500}") int enterpriseDailyLimit,
            @Value("${prompt.assistant.enterprise-credit-threshold:1000}") int enterpriseCreditThreshold,
            @Value("${prompt.assistant.timeout:5s}") Duration timeout,
            @Value("${prompt.assistant.redis-rate-limit-enabled:false}") boolean redisRateLimitEnabled) {
        this.geminiService = geminiService;
        this.promptQualityService = promptQualityService;
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
        this.promptExecutor = promptExecutor;
        this.minuteLimit = minuteLimit;
        this.freeDailyLimit = freeDailyLimit;
        this.paidDailyLimit = paidDailyLimit;
        this.enterpriseDailyLimit = enterpriseDailyLimit;
        this.enterpriseCreditThreshold = enterpriseCreditThreshold;
        this.timeout = timeout;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.redisRateLimitEnabled = redisRateLimitEnabled;
    }

    public void validateAndRecordUsage(String userId, String workspaceId) {
        int dailyLimit = resolveDailyLimit(workspaceId);
        if (redisRateLimitEnabled && redisTemplate != null) {
            try {
                validateRedisUsage(userId, workspaceId, dailyLimit);
                return;
            } catch (ResponseStatusException exception) {
                throw exception;
            } catch (RuntimeException ignored) {
                // Redis outage: preserve availability with the local single-instance limiter.
            }
        }

        Instant now = Instant.now();
        MinuteWindow minute = minuteWindows.computeIfAbsent(userId, ignored -> new MinuteWindow(now));
        if (!minute.tryAcquire(now, minuteLimit)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Prompt suggestion limit reached. Try again in one minute.");
        }

        String key = userId + ":" + workspaceId;
        DailyWindow daily = dailyWindows.computeIfAbsent(key, ignored -> new DailyWindow(LocalDate.now(ZoneOffset.UTC)));
        if (!daily.tryAcquire(LocalDate.now(ZoneOffset.UTC), dailyLimit)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Daily prompt suggestion limit reached for this subscription.");
        }
    }

    private void validateRedisUsage(String userId, String workspaceId, int dailyLimit) {
        long epochMinute = Instant.now().getEpochSecond() / 60;
        String minuteKey = "prompt:limit:minute:" + userId + ":" + epochMinute;
        Long minuteCount = redisTemplate.opsForValue().increment(minuteKey);
        if (minuteCount != null && minuteCount == 1) {
            redisTemplate.expire(minuteKey, Duration.ofMinutes(2));
        }
        if (minuteCount != null && minuteCount > minuteLimit) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Prompt suggestion limit reached. Try again in one minute.");
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String dailyKey = "prompt:limit:daily:" + userId + ":" + workspaceId + ":" + today;
        Long dailyCount = redisTemplate.opsForValue().increment(dailyKey);
        if (dailyCount != null && dailyCount == 1) {
            redisTemplate.expire(dailyKey, Duration.ofDays(2));
        }
        if (dailyCount != null && dailyCount > dailyLimit) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Daily prompt suggestion limit reached for this subscription.");
        }
    }

    @Cacheable(cacheNames = PromptCacheConfig.PROMPT_SUGGESTIONS_CACHE, key = "#request.cacheKey()", sync = true)
    public PromptSuggestionResponse generateSuggestions(PromptSuggestionRequest request) {
        PromptQuality quality = promptQualityService.evaluate(request);
        Future<PromptSuggestionResponse> future = null;
        try {
            future = promptExecutor.submit(() -> generateWithAi(request, quality));
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            if (future != null) {
                future.cancel(true);
            }
            return fallback(request, quality);
        }
    }

    public PromptSuggestionResponse qualityOnly(PromptSuggestionRequest request) {
        PromptQuality quality = promptQualityService.evaluate(request);
        PromptSuggestionResponse response = new PromptSuggestionResponse();
        response.setOriginalPrompt(request.getPromptText().trim());
        response.setOptimizedPrompt(request.getPromptText().trim());
        response.setQualityScore(quality.score());
        response.setSuggestions(quality.suggestions());
        response.setSource("RULE_BASED");
        return response;
    }

    private PromptSuggestionResponse generateWithAi(PromptSuggestionRequest request, PromptQuality quality) {
        String ruleBasedPrompt = promptQualityService.buildRuleBasedPrompt(request, quality);
        String instruction = """
                You are a prompt optimization assistant. Improve the supplied prompt without changing its intent.
                Return JSON only with this exact shape:
                {
                  "optimizedPrompt": "string",
                  "suggestions": ["short actionable suggestion"],
                  "variants": [
                    {"label": "string", "prompt": "string", "reason": "string"}
                  ]
                }
                Return no more than %d variants. Keep each prompt below 4000 characters.
                Content type: %s
                Goal: %s
                Audience: %s
                Tone: %s

                PROMPT TO IMPROVE:
                %s

                RULE-BASED DRAFT TO REFINE:
                %s
                """.formatted(
                request.getVariantCount(),
                safe(request.getContentType()),
                safe(request.getGoal()),
                safe(request.getAudience()),
                safe(request.getTone()),
                request.getPromptText().trim(),
                ruleBasedPrompt);

        String json = geminiService.generateJson(instruction);
        if (json.startsWith("Error:")) {
            return fallback(request, quality);
        }

        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(json));
            String optimized = root.path("optimizedPrompt").asText("").trim();
            if (optimized.length() < 20 || optimized.length() > 4_000) {
                return fallback(request, quality);
            }

            PromptSuggestionResponse response = new PromptSuggestionResponse();
            response.setOriginalPrompt(request.getPromptText().trim());
            response.setOptimizedPrompt(optimized);
            response.setQualityScore(quality.score());
            response.setSuggestions(readStrings(root.path("suggestions"), quality.suggestions()));
            response.setVariants(readVariants(root.path("variants"), request.getVariantCount()));
            response.setSource("AI");
            if (response.getVariants().isEmpty()) {
                response.setVariants(buildFallbackVariants(request, optimized));
            }
            return response;
        } catch (Exception exception) {
            return fallback(request, quality);
        }
    }

    private PromptSuggestionResponse fallback(PromptSuggestionRequest request, PromptQuality quality) {
        String optimized = promptQualityService.buildRuleBasedPrompt(request, quality);
        PromptSuggestionResponse response = new PromptSuggestionResponse();
        response.setOriginalPrompt(request.getPromptText().trim());
        response.setOptimizedPrompt(optimized);
        response.setQualityScore(quality.score());
        response.setSuggestions(quality.suggestions());
        response.setVariants(buildFallbackVariants(request, optimized));
        response.setSource("RULE_BASED");
        return response;
    }

    private List<PromptVariantResponse> buildFallbackVariants(
            PromptSuggestionRequest request,
            String optimized) {
        List<PromptVariantResponse> variants = new ArrayList<>();
        variants.add(new PromptVariantResponse(
                "Detailed",
                optimized,
                "Adds role, context, requirements, and a ready-to-use output instruction."));
        if (request.getVariantCount() > 1) {
            variants.add(new PromptVariantResponse(
                    "Concise",
                    "Create a clear, polished " + request.getContentType().toLowerCase()
                            + " result for this request: " + request.getPromptText().trim()
                            + ". State assumptions and return only the final usable output.",
                    "Keeps the instruction short while preserving a quality bar."));
        }
        if (request.getVariantCount() > 2) {
            variants.add(new PromptVariantResponse(
                    "Exploratory",
                    optimized + "\n\nBefore the final output, consider three creative approaches and select the strongest one.",
                    "Encourages controlled exploration before producing the answer."));
        }
        return variants;
    }

    private List<String> readStrings(JsonNode node, List<String> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String value = item.asText("").trim();
            if (!value.isBlank() && values.size() < 6) {
                values.add(value);
            }
        });
        return values.isEmpty() ? fallback : values;
    }

    private List<PromptVariantResponse> readVariants(JsonNode node, int max) {
        List<PromptVariantResponse> variants = new ArrayList<>();
        if (!node.isArray()) {
            return variants;
        }
        for (JsonNode item : node) {
            String prompt = item.path("prompt").asText("").trim();
            if (prompt.length() < 20 || prompt.length() > 4_000) {
                continue;
            }
            variants.add(new PromptVariantResponse(
                    item.path("label").asText("Alternative"),
                    prompt,
                    item.path("reason").asText("Alternative prompt structure.")));
            if (variants.size() >= max) {
                break;
            }
        }
        return variants;
    }

    private int resolveDailyLimit(String workspaceId) {
        return subscriptionRepository.findByWorkspaceIdAndStatus(workspaceId, "ACTIVE")
                .map(Subscription::getMonthlyCredits)
                .map(credits -> credits >= enterpriseCreditThreshold ? enterpriseDailyLimit : paidDailyLimit)
                .orElse(freeDailyLimit);
    }

    private String stripCodeFence(String value) {
        return value.replaceFirst("^\\s*```(?:json)?\\s*", "")
                .replaceFirst("\\s*```\\s*$", "")
                .trim();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not specified" : value.trim();
    }

    @Scheduled(fixedRateString = "${prompt.assistant.counter-cleanup-ms:3600000}")
    void cleanupExpiredCounters() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(2));
        minuteWindows.entrySet().removeIf(entry -> entry.getValue().startedAt.isBefore(cutoff));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        dailyWindows.entrySet().removeIf(entry -> entry.getValue().date.isBefore(today));
    }

    private static final class MinuteWindow {
        private Instant startedAt;
        private int count;

        private MinuteWindow(Instant startedAt) {
            this.startedAt = startedAt;
        }

        private synchronized boolean tryAcquire(Instant now, int limit) {
            if (startedAt.plusSeconds(60).isBefore(now)) {
                startedAt = now;
                count = 0;
            }
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }
    }

    private static final class DailyWindow {
        private LocalDate date;
        private int count;

        private DailyWindow(LocalDate date) {
            this.date = date;
        }

        private synchronized boolean tryAcquire(LocalDate today, int limit) {
            if (!date.equals(today)) {
                date = today;
                count = 0;
            }
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }
    }
}
