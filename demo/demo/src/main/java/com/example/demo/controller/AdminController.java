package com.example.demo.controller;

import com.example.demo.dto.AdminStatsResponse;
import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.UpdateUserRoleRequest;
import com.example.demo.entity.AiModel;
import com.example.demo.entity.AuditLog;
import com.example.demo.entity.MarketplacePost;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Workspace;
import com.example.demo.dto.MessageResponse;
import com.example.demo.service.UserAccountService;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.admin.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserAccountService userAccountService;
    private final WorkspaceAccessService workspaceAccessService;

    public AdminController(
            AdminService adminService,
            UserAccountService userAccountService,
            WorkspaceAccessService workspaceAccessService) {
        this.adminService = adminService;
        this.userAccountService = userAccountService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> users() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<AdminUserResponse> updateRole(
            @PathVariable String userId,
            @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable String userId) {
        String adminEmail = workspaceAccessService.currentUserEmail();
        return ResponseEntity.ok(userAccountService.adminDeleteUser(userId, adminEmail));
    }

    @GetMapping("/workspaces")
    public ResponseEntity<List<Workspace>> workspaces() {
        return ResponseEntity.ok(adminService.listWorkspaces());
    }

    @GetMapping("/marketplace/posts")
    public ResponseEntity<List<MarketplacePost>> marketplacePosts() {
        return ResponseEntity.ok(adminService.listMarketplacePosts());
    }

    @DeleteMapping("/marketplace/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable String postId) {
        adminService.deleteMarketplacePost(postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> auditLogs() {
        return ResponseEntity.ok(adminService.listAuditLogs());
    }

    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> payments() {
        return ResponseEntity.ok(adminService.listPayments());
    }

    @GetMapping("/ai-models")
    public ResponseEntity<List<AiModel>> aiModels() {
        return ResponseEntity.ok(adminService.listAiModels());
    }

    @PatchMapping("/ai-models/{modelId}")
    public ResponseEntity<AiModel> toggleModel(
            @PathVariable String modelId,
            @RequestBody Map<String, Boolean> body) {
        boolean active = body != null && Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(adminService.toggleAiModel(modelId, active));
    }

    @PostMapping("/users/{userId}/add-credits")
    public ResponseEntity<com.example.demo.entity.Wallet> addCredits(
            @PathVariable String userId,
            @RequestBody com.example.demo.dto.AddCreditsRequest request) {
        return ResponseEntity.ok(adminService.addUserCredits(userId, request.getAmount(), request.getDescription()));
    }

    @GetMapping("/subscription-plans")
    public ResponseEntity<List<com.example.demo.entity.SubscriptionPlan>> subscriptionPlans() {
        return ResponseEntity.ok(adminService.listSubscriptionPlans());
    }

    @PostMapping("/subscription-plans")
    public ResponseEntity<com.example.demo.entity.SubscriptionPlan> createSubscriptionPlan(
            @RequestBody com.example.demo.entity.SubscriptionPlan plan) {
        return ResponseEntity.ok(adminService.createSubscriptionPlan(plan));
    }

    @PutMapping("/subscription-plans/{planId}")
    public ResponseEntity<com.example.demo.entity.SubscriptionPlan> updateSubscriptionPlan(
            @PathVariable String planId,
            @RequestBody com.example.demo.entity.SubscriptionPlan plan) {
        return ResponseEntity.ok(adminService.updateSubscriptionPlan(planId, plan));
    }

    @DeleteMapping("/subscription-plans/{planId}")
    public ResponseEntity<Void> deleteSubscriptionPlan(@PathVariable String planId) {
        adminService.deleteSubscriptionPlan(planId);
        return ResponseEntity.noContent().build();
    }
}
