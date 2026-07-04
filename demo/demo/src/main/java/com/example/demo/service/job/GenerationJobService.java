package com.example.demo.service.job;

import com.example.demo.entity.AiModel;
import com.example.demo.entity.GeneratedContent;
import com.example.demo.entity.GenerationJob;
import com.example.demo.enums.ContentType;
import com.example.demo.enums.NotificationType;
import com.example.demo.repository.AiModelRepository;
import com.example.demo.repository.GeneratedContentRepository;
import com.example.demo.repository.GenerationJobRepository;
import com.example.demo.entity.WorkspaceMember;
import com.example.demo.entity.WorkspaceRole;
import com.example.demo.repository.WorkspaceMemberRepository;
import com.example.demo.service.ai.ContentGenerationService;
import com.example.demo.service.ai.GenerationResult;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.service.audit.AuditService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GenerationJobService {

    private final ContentGenerationService contentGenerationService;
    private final GenerationJobRepository jobRepository;
    private final GeneratedContentRepository generatedContentRepository;
    private final AiModelRepository aiModelRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public GenerationJobService(
            ContentGenerationService contentGenerationService,
            GenerationJobRepository jobRepository,
            GeneratedContentRepository generatedContentRepository,
            AiModelRepository aiModelRepository,
            AuditService auditService,
            NotificationService notificationService,
            WorkspaceMemberRepository workspaceMemberRepository) {
        this.contentGenerationService = contentGenerationService;
        this.jobRepository = jobRepository;
        this.generatedContentRepository = generatedContentRepository;
        this.aiModelRepository = aiModelRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Async
    @Transactional
    public void processAsync(String jobId) {
        GenerationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        job.setStatus("PROCESSING");
        jobRepository.save(job);

        ContentType contentType = ContentType.fromString(job.getContentType());
        Optional<AiModel> model = resolveModel(job);

        try {
            GenerationResult result = contentGenerationService.generate(
                    contentType,
                    job.getPromptText(),
                    job.getModelKey()
            );

            if (!result.isSuccess()) {
                job.setStatus("FAILED");
                job.setResult("Error: " + result.getErrorMessage());
                jobRepository.save(job);
                auditService.log(null, job.getWorkspaceId(), "GENERATION_FAILED", "GenerationJob", job.getId(), null);
                notifyWorkspaceOwner(job, true, contentType);
                return;
            }

            job.setResult(result.primaryResult());
            job.setMediaUrl(result.getMediaUrl());
            job.setStatus("COMPLETED");
            jobRepository.save(job);

            GeneratedContent content = new GeneratedContent();
            content.setGenerationJobId(job.getId());
            content.setWorkspaceId(job.getWorkspaceId());
            content.setContentType(contentType.name());
            content.setContent(buildStoredContent(result, contentType));
            content.setMediaUrl(result.getMediaUrl());
            content.setAiModelId(model.map(AiModel::getId).orElse(null));
            generatedContentRepository.save(content);

            auditService.log(null, job.getWorkspaceId(), "GENERATION_COMPLETED", "GenerationJob", job.getId(),
                    "{\"contentType\":\"" + contentType + "\"}");

            notifyWorkspaceOwner(job, false, contentType);

        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setResult("Error: " + e.getMessage());
            jobRepository.save(job);
            auditService.log(null, job.getWorkspaceId(), "GENERATION_FAILED", "GenerationJob", job.getId(),
                    "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private String buildStoredContent(GenerationResult result, ContentType type) {
        if (type == ContentType.MIXED && result.getTextContent() != null) {
            return result.getTextContent();
        }
        if (result.getTextContent() != null) {
            return result.getTextContent();
        }
        return result.getMediaUrl() != null ? result.getMediaUrl() : "";
    }

    private Optional<AiModel> resolveModel(GenerationJob job) {
        if (job.getModelKey() != null && !job.getModelKey().isBlank()) {
            return aiModelRepository.findByModelKey(job.getModelKey());
        }
        return aiModelRepository.findByContentTypeAndActiveTrue(job.getContentType())
                .stream()
                .findFirst();
    }

    private void notifyWorkspaceOwner(GenerationJob job, boolean failed, ContentType type) {
        workspaceMemberRepository.findFirstByWorkspace_IdAndRole(job.getWorkspaceId(), WorkspaceRole.OWNER)
                .map(WorkspaceMember::getUser)
                .ifPresent(user -> {
                    String label = type.name().toLowerCase();
                    String title = failed ? type.name() + " generation failed" : type.name() + " ready";
                    String message = failed
                            ? "Your " + label + " job could not be completed."
                            : "Your " + label + " content is ready to view.";
                    notificationService.notifyUser(user.getId(), title, message, NotificationType.GENERATION);
                });
    }
}
