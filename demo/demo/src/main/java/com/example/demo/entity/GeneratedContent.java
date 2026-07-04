package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "generated_contents")
public class GeneratedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "generation_job_id", nullable = false)
    private String generationJobId;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(nullable = false, length = 50)
    private String contentType = "TEXT";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "ai_model_id")
    private String aiModelId;

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGenerationJobId() { return generationJobId; }
    public void setGenerationJobId(String generationJobId) { this.generationJobId = generationJobId; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAiModelId() { return aiModelId; }
    public void setAiModelId(String aiModelId) { this.aiModelId = aiModelId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}
