package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "monthly_credits", nullable = false)
    private int monthlyCredits;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMonthlyCredits() { return monthlyCredits; }
    public void setMonthlyCredits(int monthlyCredits) { this.monthlyCredits = monthlyCredits; }

    public int getPriceCents() { return priceCents; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
