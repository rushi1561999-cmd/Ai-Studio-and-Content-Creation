package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserAccountService userAccountService;

    public AuthController(AuthService authService, UserAccountService userAccountService) {
        this.authService = authService;
        this.userAccountService = userAccountService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(userAccountService.requestPasswordReset(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userAccountService.resetPassword(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {
        return ResponseEntity.ok(userAccountService.getCurrentUser());
    }

    @PutMapping("/me")
    public ResponseEntity<UpdateUserResponse> updateMe(@RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userAccountService.updateCurrentUser(request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deleteMe(@RequestBody DeleteAccountRequest request) {
        return ResponseEntity.ok(userAccountService.deleteCurrentUser(request));
    }
}
