package com.example.demo.dto;

public class AdminStatsResponse {
    private long totalUsers;
    private long totalWorkspaces;
    private long totalMarketplacePosts;
    private long totalGenerationJobs;
    private long totalPayments;
    private long totalCreditsInWallets;

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalWorkspaces() { return totalWorkspaces; }
    public void setTotalWorkspaces(long totalWorkspaces) { this.totalWorkspaces = totalWorkspaces; }

    public long getTotalMarketplacePosts() { return totalMarketplacePosts; }
    public void setTotalMarketplacePosts(long totalMarketplacePosts) {
        this.totalMarketplacePosts = totalMarketplacePosts;
    }

    public long getTotalGenerationJobs() { return totalGenerationJobs; }
    public void setTotalGenerationJobs(long totalGenerationJobs) {
        this.totalGenerationJobs = totalGenerationJobs;
    }

    public long getTotalPayments() { return totalPayments; }
    public void setTotalPayments(long totalPayments) { this.totalPayments = totalPayments; }

    public long getTotalCreditsInWallets() { return totalCreditsInWallets; }
    public void setTotalCreditsInWallets(long totalCreditsInWallets) {
        this.totalCreditsInWallets = totalCreditsInWallets;
    }
}
