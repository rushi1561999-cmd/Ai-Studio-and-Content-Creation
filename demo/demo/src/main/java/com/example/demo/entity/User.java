package com.example.demo.entity;

import com.example.demo.enums.PlatformRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // Will store the BCrypt hash

    private String fullName;
    
    private String googleId; // For OAuth2 later

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_role", nullable = false)
    private PlatformRole platformRole = PlatformRole.USER;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and Setters...
 // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public PlatformRole getPlatformRole() { return platformRole; }
    public void setPlatformRole(PlatformRole platformRole) { this.platformRole = platformRole; }

    public boolean isAdmin() {
        return platformRole == PlatformRole.ADMIN;
    }
}