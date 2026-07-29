package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private String workspaceId;
    
    private int credits;

    public Wallet() {}

    public Wallet(String workspaceId, int credits) {
        this.workspaceId = workspaceId;
        this.credits = credits;
    }

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
}
