package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_posts")
public class MarketplacePost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(columnDefinition = "TEXT")
    private String promptText;
    
    private String category;
    private String authorName;
    private int likes = 0;
    
    private LocalDateTime createdAt = LocalDateTime.now();

    // Default Constructor
    public MarketplacePost() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}