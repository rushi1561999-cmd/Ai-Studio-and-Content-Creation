package com.example.demo.controller;

import com.example.demo.dto.ContentTypeInfo;
import com.example.demo.dto.GenerateContentRequest;
import com.example.demo.entity.GenerationJob;
import com.example.demo.entity.Wallet;
import com.example.demo.enums.ContentType;
import com.example.demo.queue.AiJobProducer;
import com.example.demo.repository.GenerationJobRepository;
import com.example.demo.repository.GeneratedContentRepository;
import com.example.demo.service.GenerationJobProcessor;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.billing.WalletBillingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiJobProducer aiJobProducer;
    private final GenerationJobRepository jobRepository;
    private final WalletBillingService walletBillingService;
    private final GenerationJobProcessor jobProcessor;
    private final WorkspaceAccessService workspaceAccessService;
    private final GeneratedContentRepository generatedContentRepository;
    private final boolean rabbitMqEnabled;

    private static final int MAX_TOPUP = 10_000;

    public AiController(
            AiJobProducer aiJobProducer,
            GenerationJobRepository jobRepository,
            WalletBillingService walletBillingService,
            GenerationJobProcessor jobProcessor,
            WorkspaceAccessService workspaceAccessService,
            GeneratedContentRepository generatedContentRepository,
            @Value("${ai.processing.rabbitmq:false}") boolean rabbitMqEnabled) {
        this.aiJobProducer = aiJobProducer;
        this.jobRepository = jobRepository;
        this.walletBillingService = walletBillingService;
        this.jobProcessor = jobProcessor;
        this.workspaceAccessService = workspaceAccessService;
        this.generatedContentRepository = generatedContentRepository;
        this.rabbitMqEnabled = rabbitMqEnabled;
    }

    @GetMapping("/content-types")
    public ResponseEntity<List<ContentTypeInfo>> contentTypes() {
        List<ContentTypeInfo> types = Arrays.asList(
                new ContentTypeInfo("TEXT", "Text", "Articles, copy, explanations", ContentType.TEXT.getCreditCost()),
                new ContentTypeInfo("IMAGE", "Image", "AI images from your prompt", ContentType.IMAGE.getCreditCost()),
                new ContentTypeInfo("VIDEO", "Video", "Short AI video clips", ContentType.VIDEO.getCreditCost()),
                new ContentTypeInfo("MIXED", "Rich content", "Text + image combined", ContentType.MIXED.getCreditCost())
        );
        return ResponseEntity.ok(types);
    }

    @GetMapping("/wallet/{workspaceId}")
    public ResponseEntity<Wallet> getWalletBalance(@PathVariable String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return ResponseEntity.ok(walletBillingService.getOrCreateWallet(workspaceId));
    }

    @PostMapping("/wallet/{workspaceId}/topup")
    public ResponseEntity<Wallet> topUpCredits(
            @PathVariable String workspaceId,
            @RequestParam int amount) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);

        if (amount <= 0 || amount > MAX_TOPUP) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(walletBillingService.credit(
                workspaceId,
                amount,
                com.example.demo.enums.CreditTransactionType.ADJUSTMENT,
                null,
                "Manual top-up"));
    }

    @PostMapping("/generate")
    @Transactional
    public ResponseEntity<?> generateAiContent(@RequestBody GenerateContentRequest request) {
        if (request.getPromptText() == null || request.getPromptText().isBlank()) {
            return ResponseEntity.badRequest().body("Prompt text is required.");
        }
        if (request.getWorkspaceId() == null || request.getWorkspaceId().isBlank()) {
            return ResponseEntity.badRequest().body("Workspace ID is required.");
        }

        workspaceAccessService.requireWorkspaceAccess(request.getWorkspaceId());

        ContentType contentType = ContentType.fromString(request.getContentType());
        int creditCost = contentType.getCreditCost();

        walletBillingService.getOrCreateWallet(request.getWorkspaceId());

        GenerationJob job = aiJobProducer.submitJob(
                request.getPromptText().trim(),
                request.getWorkspaceId(),
                contentType,
                request.getModelKey()
        );

        try {
            walletBillingService.debit(
                    request.getWorkspaceId(),
                    creditCost,
                    job.getId(),
                    "AI " + contentType.name().toLowerCase() + " generation"
            );
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                job.setStatus("FAILED");
                job.setResult("Insufficient credits");
                jobRepository.save(job);
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body("Insufficient credits. This " + contentType.name().toLowerCase()
                                + " generation requires " + creditCost + " credits.");
            }
            throw ex;
        }

        if (!rabbitMqEnabled) {
            String jobId = job.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    jobProcessor.processAsync(jobId, request.getPromptText());
                }
            });
        }

        return ResponseEntity.ok(job);
    }

    /** Legacy query-param endpoint for backward compatibility */
    @PostMapping("/generate/simple")
    @Transactional
    public ResponseEntity<?> generateSimple(
            @RequestParam String promptText,
            @RequestParam String workspaceId) {
        GenerateContentRequest request = new GenerateContentRequest();
        request.setPromptText(promptText);
        request.setWorkspaceId(workspaceId);
        request.setContentType("TEXT");
        return generateAiContent(request);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        GenerationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        workspaceAccessService.requireWorkspaceAccess(job.getWorkspaceId());
        return ResponseEntity.ok(job);
    }

    @GetMapping("/jobs/workspace/{workspaceId}")
    public ResponseEntity<?> getWorkspaceHistory(@PathVariable String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return ResponseEntity.ok(jobRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId));
    }

    @GetMapping("/contents/workspace/{workspaceId}")
    public ResponseEntity<?> getGeneratedContents(@PathVariable String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return ResponseEntity.ok(generatedContentRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId));
    }
}
