package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.WorkspaceRequest;
import com.example.demo.entity.User;
import com.example.demo.enums.PlatformRole;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WorkspaceService workspaceService;
    private final EmailService emailService;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            WorkspaceService workspaceService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.workspaceService = workspaceService;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use.");
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPlatformRole(PlatformRole.USER);

        userRepository.save(user);

        WorkspaceRequest workspaceRequest = new WorkspaceRequest();
        String displayName = request.getFullName() != null && !request.getFullName().isBlank()
                ? request.getFullName()
                : "My";
        workspaceRequest.setName(displayName + "'s Workspace");
        workspaceService.createWorkspace(workspaceRequest, user.getEmail());

        // Send registration email
        if (emailEnabled) {
            try {
                emailService.sendRegistrationEmail(user.getEmail(), user.getFullName());
            } catch (Exception e) {
                System.err.println("[AuthService] Failed to send registration email: " + e.getMessage());
            }
        }

        return buildAuthResponse(user, "User successfully registered!");
    }

    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        // Send login notification email
        if (emailEnabled) {
            try {
                String loginTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
                emailService.sendLoginNotification(user.getEmail(), user.getFullName(), loginTime);
            } catch (Exception e) {
                System.err.println("[AuthService] Failed to send login notification email: " + e.getMessage());
            }
        }

        return buildAuthResponse(user, "Login successful!");
    }

    private AuthResponse buildAuthResponse(User user, String message) {
        String jwt = jwtService.generateToken(user.getEmail(), user.getPlatformRole());
        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setMessage(message);
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getPlatformRole().name());
        return response;
    }
}
