package com.example.demo.entity;

import com.example.demo.enums.CreditTransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_transactions")
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTransactionType type;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public CreditTransactionType getType() { return type; }
    public void setType(CreditTransactionType type) { this.type = type; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(int balanceAfter) { this.balanceAfter = balanceAfter; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
