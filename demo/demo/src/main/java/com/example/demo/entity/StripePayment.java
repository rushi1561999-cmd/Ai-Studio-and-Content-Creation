package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_payments")
public class StripePayment {

    @Id
    private String sessionId;

    @Column(nullable = false)
    private String workspaceId;

    private int credits;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime paidAt;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
