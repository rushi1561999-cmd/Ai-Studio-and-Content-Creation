package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.enums.PlatformRole;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(name = "app.admin.seed.enabled", havingValue = "true")
public class AdminDataInitializer {

    @Bean
    CommandLineRunner seedAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword,
            @Value("${app.admin.name:Platform Admin}") String adminName) {
        return args -> {
            String email = adminEmail.trim().toLowerCase();
            if (email.isBlank() || !email.contains("@")) {
                throw new IllegalStateException("APP_ADMIN_EMAIL must be a valid email when admin seeding is enabled.");
            }
            if (adminPassword == null || adminPassword.length() < 10) {
                throw new IllegalStateException("APP_ADMIN_PASSWORD must contain at least 10 characters.");
            }
            userRepository.findByEmail(email).ifPresentOrElse(
                    user -> {
                        if (user.getPlatformRole() != PlatformRole.ADMIN) {
                            user.setPlatformRole(PlatformRole.ADMIN);
                            userRepository.save(user);
                        }
                    },
                    () -> {
                        User admin = new User();
                        admin.setEmail(email);
                        admin.setPassword(passwordEncoder.encode(adminPassword));
                        admin.setFullName(adminName);
                        admin.setPlatformRole(PlatformRole.ADMIN);
                        userRepository.save(admin);
                    }
            );
        };
    }
}
