package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "generation_jobs")
public class GenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String promptText;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType = "TEXT";

    @Column(name = "model_key", length = 100)
    private String modelKey;

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}
