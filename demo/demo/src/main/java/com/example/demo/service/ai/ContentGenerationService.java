package com.example.demo.service.ai;

import com.example.demo.entity.AiModel;
import com.example.demo.enums.ContentType;
import com.example.demo.repository.AiModelRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ContentGenerationService {

    private final GeminiService geminiService;
    private final ReplicateService replicateService;
    private final AiModelRepository aiModelRepository;

    public ContentGenerationService(
            GeminiService geminiService,
            ReplicateService replicateService,
            AiModelRepository aiModelRepository) {
        this.geminiService = geminiService;
        this.replicateService = replicateService;
        this.aiModelRepository = aiModelRepository;
    }

    public GenerationResult generate(ContentType type, String prompt, String modelKey) {
        return switch (type) {
            case TEXT -> generateText(prompt, modelKey);
            case IMAGE -> generateImage(prompt, modelKey);
            case VIDEO -> generateVideo(prompt, modelKey);
            case MIXED -> generateMixed(prompt, modelKey);
        };
    }

    private GenerationResult generateText(String prompt, String modelKey) {
        resolveModel(modelKey, ContentType.TEXT);
        String text = geminiService.generateText(prompt);
        if (text.startsWith("Error:")) {
            return GenerationResult.fail(text.substring(7).trim());
        }
        return GenerationResult.ok(text);
    }

    private GenerationResult generateImage(String prompt, String modelKey) {
        AiModel model = resolveModel(modelKey, ContentType.IMAGE);
        if (model != null && model.getProvider().name().equals("GEMINI")) {
            String text = geminiService.generateText(
                    "Describe an image in one paragraph for: " + prompt);
            if (!text.startsWith("Error:")) {
                GenerationResult img = replicateService.generateImage(prompt);
                if (img.isSuccess()) {
                    return GenerationResult.okMedia(img.getMediaUrl());
                }
            }
        }
        return replicateService.generateImage(prompt);
    }

    private GenerationResult generateVideo(String prompt, String modelKey) {
        resolveModel(modelKey, ContentType.VIDEO);
        return replicateService.generateVideo(prompt);
    }

    private GenerationResult generateMixed(String prompt, String modelKey) {
        resolveModel(modelKey, ContentType.MIXED);

        String text = geminiService.generateText(
                "Create detailed creative content (title, description, and bullet points) for: " + prompt);
        if (text.startsWith("Error:")) {
            return GenerationResult.fail(text.substring(7).trim());
        }

        GenerationResult image = replicateService.generateImage(prompt);
        if (!image.isSuccess()) {
            return GenerationResult.ok(text);
        }

        return GenerationResult.okMixed(text, image.getMediaUrl());
    }

    private AiModel resolveModel(String modelKey, ContentType type) {
        if (modelKey != null && !modelKey.isBlank()) {
            Optional<AiModel> model = aiModelRepository.findByModelKey(modelKey);
            if (model.isPresent() && model.get().isActive()) {
                return model.get();
            }
        }
        return aiModelRepository.findByContentTypeAndActiveTrue(type.name())
                .stream()
                .findFirst()
                .orElse(null);
    }
}
