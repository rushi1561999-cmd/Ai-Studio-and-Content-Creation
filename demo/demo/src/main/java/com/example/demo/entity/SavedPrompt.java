package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_prompts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "post_id"})
})
public class SavedPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private MarketplacePost post;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime savedAt;

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public MarketplacePost getPost() { return post; }
    public void setPost(MarketplacePost post) { this.post = post; }

    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
}
