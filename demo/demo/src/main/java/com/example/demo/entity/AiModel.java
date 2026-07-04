package com.example.demo.entity;

import com.example.demo.enums.AiProvider;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_models")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiProvider provider;

    @Column(name = "model_key", nullable = false, unique = true)
    private String modelKey;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "credits_per_use", nullable = false)
    private int creditsPerUse = 1;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType = "TEXT";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public AiProvider getProvider() { return provider; }
    public void setProvider(AiProvider provider) { this.provider = provider; }

    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getCreditsPerUse() { return creditsPerUse; }
    public void setCreditsPerUse(int creditsPerUse) { this.creditsPerUse = creditsPerUse; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
