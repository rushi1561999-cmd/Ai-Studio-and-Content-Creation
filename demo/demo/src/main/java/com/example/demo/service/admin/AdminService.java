package com.example.demo.service.admin;

import com.example.demo.dto.AdminStatsResponse;

import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.UpdateUserRoleRequest;
import com.example.demo.entity.*;
import com.example.demo.enums.PlatformRole;
import com.example.demo.enums.CreditTransactionType;
import com.example.demo.repository.*;
import com.example.demo.service.billing.WalletBillingService;
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
    private final SubscriptionRepository subscriptionRepository;
    private final WalletBillingService walletBillingService;

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
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionRepository subscriptionRepository,
            WalletBillingService walletBillingService) {
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
        this.subscriptionRepository = subscriptionRepository;
        this.walletBillingService = walletBillingService;
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

        if (amount <= 0 || amount > 100_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be between 1 and 100000 credits.");
        }
        String reason = description == null ? "" : description.trim();
        if (reason.isBlank() || reason.length() > 240) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason of 1 to 240 characters is required.");
        }

        Workspace workspace = workspaceMemberRepository.findFirstByUser_IdOrderByJoinedAtAsc(user.getId())
                .map(WorkspaceMember::getWorkspace)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User has no workspace."));

        return walletBillingService.credit(
                workspace.getId(),
                amount,
                CreditTransactionType.ADJUSTMENT,
                "admin:" + user.getId(),
                reason);
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
        String normalizedCode = plan.getCode().trim().toLowerCase();
        if (subscriptionPlanRepository.findByCode(normalizedCode).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan code already exists.");
        }
        if (plan.getCurrency() == null || plan.getCurrency().trim().isEmpty()) {
            plan.setCurrency("INR");
        }
        plan.setCurrency(plan.getCurrency().trim().toUpperCase());
        if (!plan.getCurrency().equals("USD") && !plan.getCurrency().equals("INR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be USD or INR.");
        }
        if (plan.getName() == null || plan.getName().isBlank() || plan.getName().length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan name must contain 1 to 120 characters.");
        }
        if (plan.getMonthlyCredits() <= 0 || plan.getPriceCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paid plans require positive credits and price.");
        }
        plan.setCode(normalizedCode);
        plan.setName(plan.getName().trim());
        return subscriptionPlanRepository.save(plan);
    }

    @Transactional
    public SubscriptionPlan updateSubscriptionPlan(String planId, SubscriptionPlan plan) {
        SubscriptionPlan existing = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found."));

        if (plan.getCode() != null && !plan.getCode().trim().isEmpty()) {
            String normalizedCode = plan.getCode().trim().toLowerCase();
            if (!existing.getCode().equals(normalizedCode) &&
                subscriptionPlanRepository.findByCode(normalizedCode).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan code already exists.");
            }
            existing.setCode(normalizedCode);
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
            String currency = plan.getCurrency().trim().toUpperCase();
            if (!currency.equals("USD") && !currency.equals("INR")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be USD or INR.");
            }
            existing.setCurrency(currency);
        }
        existing.setActive(plan.isActive());

        return subscriptionPlanRepository.save(existing);
    }

    @Transactional
    public void deleteSubscriptionPlan(String planId) {
        if (!subscriptionPlanRepository.existsById(planId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found.");
        }
        if (subscriptionRepository.existsByPlanId(planId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deactivate plans that have subscription history instead of deleting them.");
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
