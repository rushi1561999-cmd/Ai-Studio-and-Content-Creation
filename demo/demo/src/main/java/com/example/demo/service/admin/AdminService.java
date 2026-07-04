package com.example.demo.service.admin;

import com.example.demo.dto.AdminStatsResponse;

import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.UpdateUserRoleRequest;
import com.example.demo.entity.*;
import com.example.demo.enums.PlatformRole;
import com.example.demo.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MarketplacePostRepository marketplacePostRepository;
    private final GenerationJobRepository generationJobRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final AuditLogRepository auditLogRepository;
    private final AiModelRepository aiModelRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final SavedPromptRepository savedPromptRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public AdminService(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            MarketplacePostRepository marketplacePostRepository,
            GenerationJobRepository generationJobRepository,
            PaymentRepository paymentRepository,
            WalletRepository walletRepository,
            AuditLogRepository auditLogRepository,
            AiModelRepository aiModelRepository,
            PostLikeRepository postLikeRepository,
            CommentRepository commentRepository,
            SavedPromptRepository savedPromptRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            SubscriptionPlanRepository subscriptionPlanRepository) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.marketplacePostRepository = marketplacePostRepository;
        this.generationJobRepository = generationJobRepository;
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
        this.auditLogRepository = auditLogRepository;
        this.aiModelRepository = aiModelRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.savedPromptRepository = savedPromptRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        AdminStatsResponse stats = new AdminStatsResponse();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalWorkspaces(workspaceRepository.count());
        stats.setTotalMarketplacePosts(marketplacePostRepository.count());
        stats.setTotalGenerationJobs(generationJobRepository.count());
        stats.setTotalPayments(paymentRepository.count());
        stats.setTotalCreditsInWallets(
                walletRepository.findAll().stream().mapToLong(Wallet::getCredits).sum()
        );
        return stats;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toAdminUser)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminUserResponse updateUserRole(String userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        PlatformRole role;
        try {
            role = PlatformRole.valueOf(request.getRole().trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or ADMIN.");
        }

        user.setPlatformRole(role);
        return toAdminUser(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<Workspace> listWorkspaces() {
        return workspaceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MarketplacePost> listMarketplacePosts() {
        return marketplacePostRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void deleteMarketplacePost(String postId) {
        if (!marketplacePostRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found.");
        }
        postLikeRepository.deleteByPost_Id(postId);
        commentRepository.deleteByPost_Id(postId);
        savedPromptRepository.deleteByPost_Id(postId);
        marketplacePostRepository.deleteById(postId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listAuditLogs() {
        return auditLogRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(200)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Payment> listPayments() {
        return paymentRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AiModel> listAiModels() {
        return aiModelRepository.findAll();
    }

    @Transactional
    public AiModel toggleAiModel(String modelId, boolean active) {
        AiModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Model not found."));
        model.setActive(active);
        return aiModelRepository.save(model);
    }

    @Transactional
    public Wallet addUserCredits(String userId, int amount, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive.");
        }

        // Get user's workspace (assuming first workspace or create one)
        Workspace workspace = workspaceRepository.findAll().stream()
                .filter(ws -> workspaceMemberRepository.existsByUser_IdAndWorkspace_Id(user.getId(), ws.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User has no workspace."));

        // Add credits to the workspace wallet
        Wallet wallet = walletRepository.findById(workspace.getId())
                .orElse(new Wallet(workspace.getId(), 0));

        wallet.setCredits(wallet.getCredits() + amount);
        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> listSubscriptionPlans() {
        return subscriptionPlanRepository.findAll();
    }

    @Transactional
    public SubscriptionPlan createSubscriptionPlan(SubscriptionPlan plan) {
        if (plan.getCode() == null || plan.getCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan code is required.");
        }
        if (subscriptionPlanRepository.findByCode(plan.getCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan code already exists.");
        }
        if (plan.getCurrency() == null || plan.getCurrency().trim().isEmpty()) {
            plan.setCurrency("USD");
        }
        if (!plan.getCurrency().equals("USD") && !plan.getCurrency().equals("INR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be USD or INR.");
        }
        return subscriptionPlanRepository.save(plan);
    }

    @Transactional
    public SubscriptionPlan updateSubscriptionPlan(String planId, SubscriptionPlan plan) {
        SubscriptionPlan existing = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found."));

        if (plan.getCode() != null && !plan.getCode().trim().isEmpty()) {
            if (!existing.getCode().equals(plan.getCode()) &&
                subscriptionPlanRepository.findByCode(plan.getCode()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan code already exists.");
            }
            existing.setCode(plan.getCode());
        }

        if (plan.getName() != null) {
            existing.setName(plan.getName());
        }
        if (plan.getMonthlyCredits() > 0) {
            existing.setMonthlyCredits(plan.getMonthlyCredits());
        }
        if (plan.getPriceCents() >= 0) {
            existing.setPriceCents(plan.getPriceCents());
        }
        if (plan.getCurrency() != null && !plan.getCurrency().trim().isEmpty()) {
            if (!plan.getCurrency().equals("USD") && !plan.getCurrency().equals("INR")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be USD or INR.");
            }
            existing.setCurrency(plan.getCurrency());
        }
        existing.setActive(plan.isActive());

        return subscriptionPlanRepository.save(existing);
    }

    @Transactional
    public void deleteSubscriptionPlan(String planId) {
        if (!subscriptionPlanRepository.existsById(planId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found.");
        }
        subscriptionPlanRepository.deleteById(planId);
    }

    private AdminUserResponse toAdminUser(User user) {
        AdminUserResponse dto = new AdminUserResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getPlatformRole().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
