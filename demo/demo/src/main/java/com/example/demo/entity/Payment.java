package com.example.demo.entity;

import com.example.demo.enums.PaymentProvider;
import com.example.demo.enums.PaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Column(name = "external_id")
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "credits_granted")
    private int creditsGranted;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentProvider getProvider() { return provider; }
    public void setProvider(PaymentProvider provider) { this.provider = provider; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public int getCreditsGranted() { return creditsGranted; }
    public void setCreditsGranted(int creditsGranted) { this.creditsGranted = creditsGranted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
