package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private String workspaceId; // We map the wallet directly to the user's workspace
    
    private int credits;

    // Default Constructor
    public Wallet() {}

    public Wallet(String workspaceId, int credits) {
        this.workspaceId = workspaceId;
        this.credits = credits;
    }

    // Getters and Setters
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
}