package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.User;
import com.example.demo.enums.PlatformRole;
import com.example.demo.repository.*;
import com.example.demo.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class UserAccountService {

    private static final int RESET_TOKEN_BYTES = 32;
    private static final int RESET_EXPIRY_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final NotificationRepository notificationRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final SavedPromptRepository savedPromptRepository;
    private final FollowerRepository followerRepository;
    private final PromptRepository promptRepository;
    private final PromptHistoryRepository promptHistoryRepository;
    private final AiJobRepository aiJobRepository;
    private final JwtService jwtService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.password-reset.expose-token:true}")
    private boolean exposeResetToken;

    public UserAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository resetTokenRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            NotificationRepository notificationRepository,
            PostLikeRepository postLikeRepository,
            CommentRepository commentRepository,
            SavedPromptRepository savedPromptRepository,
            FollowerRepository followerRepository,
            PromptRepository promptRepository,
            PromptHistoryRepository promptHistoryRepository,
            AiJobRepository aiJobRepository,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resetTokenRepository = resetTokenRepository;
        this.jwtService = jwtService;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.notificationRepository = notificationRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.savedPromptRepository = savedPromptRepository;
        this.followerRepository = followerRepository;
        this.promptRepository = promptRepository;
        this.promptHistoryRepository = promptHistoryRepository;
        this.aiJobRepository = aiJobRepository;
    }

    public UserProfileResponse getCurrentUser() {
        return toProfile(requireCurrentUser());
    }

    @Transactional
    public UpdateUserResponse updateCurrentUser(UpdateUserRequest request) {
        User user = requireCurrentUser();
        boolean credentialsChanged = false;

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use.");
                }
                credentialsChanged = true;
            }
            user.setEmail(newEmail);
        }

        boolean changingPassword = request.getNewPassword() != null && !request.getNewPassword().isBlank();
        if (changingPassword) {
            credentialsChanged = true;
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required.");
            }
            if (request.getNewPassword().length() < 6) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters.");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        User saved = userRepository.save(user);
        UpdateUserResponse response = new UpdateUserResponse();
        response.setUser(toProfile(saved));
        response.setMessage("Profile updated successfully.");
        if (credentialsChanged) {
            response.setToken(jwtService.generateToken(saved.getEmail(), saved.getPlatformRole()));
            response.setMessage("Profile updated. Use the new session token if your email or password changed.");
        }
        return response;
    }

    @Transactional
    public MessageResponse deleteCurrentUser(DeleteAccountRequest request) {
        User user = requireCurrentUser();

        if (user.getPlatformRole() == PlatformRole.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getPlatformRole() == PlatformRole.ADMIN)
                    .count();
            if (adminCount <= 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot delete the only platform admin account."
                );
            }
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required to delete your account.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is incorrect.");
        }

        deleteUserData(user.getId());
        userRepository.delete(user);

        return new MessageResponse("Account deleted successfully.");
    }

    @Transactional
    public PasswordResetResponse requestPasswordReset(ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }

        String email = request.getEmail().trim().toLowerCase();
        String genericMessage = "If an account exists for that email, password reset instructions have been sent.";

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return new PasswordResetResponse(genericMessage, null);
        }

        User user = userOpt.get();
        resetTokenRepository.deleteByUserId(user.getId());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(generateSecureToken());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(RESET_EXPIRY_HOURS));
        resetTokenRepository.save(resetToken);

        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken.getToken();
        System.out.println("[Password reset] " + email + " -> " + resetUrl);

        if (exposeResetToken) {
            return new PasswordResetResponse(genericMessage, resetUrl);
        }
        return new PasswordResetResponse(genericMessage, null);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is required.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }

        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenAndUsedFalse(request.getToken().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token."));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has expired. Request a new one.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        return new MessageResponse("Password updated successfully. You can log in with your new password.");
    }

    @Transactional
    public MessageResponse adminDeleteUser(String userId, String adminEmail) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found."));

        if (target.getId().equals(admin.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own admin account here.");
        }

        if (target.getPlatformRole() == PlatformRole.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getPlatformRole() == PlatformRole.ADMIN)
                    .count();
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the only admin.");
            }
        }

        deleteUserData(target.getId());
        userRepository.delete(target);

        return new MessageResponse("User deleted.");
    }

    private void deleteUserData(String userId) {
        resetTokenRepository.deleteByUserId(userId);
        promptHistoryRepository.deleteByUser_Id(userId);
        promptRepository.deleteByCreatedBy_Id(userId);
        postLikeRepository.deleteByUser_Id(userId);
        commentRepository.deleteByUser_Id(userId);
        savedPromptRepository.deleteByUser_Id(userId);
        followerRepository.deleteByFollower_Id(userId);
        followerRepository.deleteByFollowing_Id(userId);
        notificationRepository.deleteByUserId(userId);
        aiJobRepository.deleteByUserId(userId);
        workspaceMemberRepository.deleteByUser_Id(userId);
    }

    private User requireCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private UserProfileResponse toProfile(User user) {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(user.getId());
        profile.setEmail(user.getEmail());
        profile.setFullName(user.getFullName());
        profile.setRole(user.getPlatformRole().name());
        return profile;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
