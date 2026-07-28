package com.example.demo.service.job;

import com.example.demo.dto.JobStatusEvent;
import com.example.demo.entity.GenerationJob;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class JobEventService {

    private static final long STREAM_TIMEOUT_MILLIS = 30L * 60L * 1_000L;
    public static final String REDIS_CHANNEL = "ai:job-status";
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> workspaceEmitters =
            new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final JsonMapper objectMapper;
    private final boolean redisEnabled;

    public JobEventService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            JsonMapper objectMapper,
            @Value("${ai.job-stream.redis-enabled:false}") boolean redisEnabled) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        this.redisEnabled = redisEnabled;
    }

    public SseEmitter subscribe(String workspaceId, GenerationJob initialJob) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        workspaceEmitters.computeIfAbsent(workspaceId, ignored -> new CopyOnWriteArrayList<>())
                .add(emitter);
        emitter.onCompletion(() -> remove(workspaceId, emitter));
        emitter.onTimeout(() -> remove(workspaceId, emitter));
        emitter.onError(error -> remove(workspaceId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("workspaceId", workspaceId)));
            if (initialJob != null) {
                emitter.send(SseEmitter.event().name("job-status").data(JobStatusEvent.from(initialJob)));
            }
        } catch (IOException exception) {
            remove(workspaceId, emitter);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void publish(GenerationJob job) {
        JobStatusEvent event = JobStatusEvent.from(job);
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.convertAndSend(REDIS_CHANNEL, objectMapper.writeValueAsString(event));
                return;
            } catch (Exception ignored) {
                // Redis outage: deliver to clients connected to this instance.
            }
        }
        publishLocal(event);
    }

    public void publishLocal(JobStatusEvent event) {
        List<SseEmitter> emitters = workspaceEmitters.get(event.workspaceId());
        if (emitters == null) {
            return;
        }
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("job-status").data(event));
            } catch (IOException exception) {
                remove(event.workspaceId(), emitter);
            }
        });
    }

    @Scheduled(fixedRateString = "${ai.job-stream.heartbeat-ms:15000}")
    void heartbeat() {
        workspaceEmitters.forEach((workspaceId, emitters) ->
                emitters.forEach(emitter -> {
                    try {
                        emitter.send(SseEmitter.event().comment("keep-alive"));
                    } catch (IOException exception) {
                        remove(workspaceId, emitter);
                    }
                }));
    }

    private void remove(String workspaceId, SseEmitter emitter) {
        List<SseEmitter> emitters = workspaceEmitters.get(workspaceId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            workspaceEmitters.remove(workspaceId, emitters);
        }
    }
}
