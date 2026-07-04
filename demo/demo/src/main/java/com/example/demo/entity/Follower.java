package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "followers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"follower_id", "following_id"})
})
public class Follower {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // The person who clicked "Follow"
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    // The creator they are following
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime followedAt;

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public User getFollower() { return follower; }
    public void setFollower(User follower) { this.follower = follower; }

    public User getFollowing() { return following; }
    public void setFollowing(User following) { this.following = following; }

    public LocalDateTime getFollowedAt() { return followedAt; }
    public void setFollowedAt(LocalDateTime followedAt) { this.followedAt = followedAt; }
}
