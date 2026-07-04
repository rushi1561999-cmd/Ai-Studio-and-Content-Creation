package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_scores")
public class PromptScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private Prompt prompt;

    @Column(nullable = false)
    private Double score; // e.g., 1.0 to 10.0 or a percentage

    @Column(columnDefinition = "TEXT")
    private String feedback; // Optional user notes on why it scored well/poorly

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Prompt getPrompt() { return prompt; }
    public void setPrompt(Prompt prompt) { this.prompt = prompt; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}