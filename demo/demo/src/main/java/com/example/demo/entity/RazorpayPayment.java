package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "razorpay_payments")
public class RazorpayPayment {

    @Id
    private String orderId;

    @Column(nullable = false)
    private String workspaceId;

    @Column(nullable = false)
    private String paymentId;

    private int credits;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime paidAt;

    // Constructors
    public RazorpayPayment() {}

    public RazorpayPayment(String orderId, String workspaceId, String paymentId, int credits) {
        this.orderId = orderId;
        this.workspaceId = workspaceId;
        this.paymentId = paymentId;
        this.credits = credits;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
